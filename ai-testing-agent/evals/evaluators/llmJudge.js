/**
 * UMSS Market - LLM as a Judge
 *
 * Evalúa la calidad textual de las respuestas del Golden Dataset.
 *
 * IMPORTANTE:
 *
 * El evaluador determinista es responsable de verificar:
 * - invocación del proveedor
 * - selección de herramientas
 * - ejecución de herramientas
 * - aislamiento de usuarios
 * - búsquedas
 * - políticas observables
 *
 * El LLM Judge NO intenta inferir esas propiedades.
 *
 * El LLM Judge evalúa únicamente aspectos que puede observar
 * directamente en la respuesta textual:
 * - contenido requerido
 * - contenido prohibido
 * - grounding explícito cuando exista
 *
 * El modelo NO calcula el score final.
 * Devuelve observaciones binarias por criterio.
 * Este archivo calcula el score.
 */

import axios from "axios";


/* =========================================================
   CONFIGURATION
   ========================================================= */

const OLLAMA_URL =
  process.env.OLLAMA_URL ||
  "http://localhost:11434";

const OLLAMA_MODEL =
  process.env.OLLAMA_MODEL ||
  "qwen2.5:7b";

const REQUEST_TIMEOUT =
  Number(
    process.env.LLM_JUDGE_TIMEOUT ||
    30000
  );


/* =========================================================
   MAIN DATASET JUDGE
   ========================================================= */

export async function judgeDataset(
  executions = []
) {
  const results = [];

  for (const execution of executions) {
    try {
      const result =
        await judgeExecution(execution);

      results.push(result);

    } catch (error) {
      results.push({
        execution_id:
          execution.execution_id,

        case_id:
          execution.testCase.id,

        available:
          false,

        error:
          error.message,

        score:
          null,

        criteria:
          []
      });
    }
  }

  /*
   * Solo utilizamos scores válidos
   * para calcular el promedio.
   */

  const validScores =
    results
      .map(
        (result) =>
          result.score
      )
      .filter(
        (score) =>
          typeof score === "number" &&
          Number.isFinite(score)
      );

  const average =
    validScores.length === 0
      ? null
      : validScores.reduce(
          (sum, score) =>
            sum + score,
          0
        ) /
        validScores.length;

  return {
    available:
      validScores.length > 0,

    total:
      results.length,

    evaluated:
      validScores.length,

    failed:
      results.length -
      validScores.length,

    average:
      average === null
        ? null
        : round(average),

    score:
      average === null
        ? null
        : round(average),

    results
  };
}


/* =========================================================
   SINGLE EXECUTION
   ========================================================= */

async function judgeExecution(
  execution
) {
  const testCase =
    execution.testCase;

  const response =
    String(
      execution.response || ""
    );

  /*
   * El juez solamente recibe criterios que puede evaluar
   * directamente desde el contenido textual.
   */
  const observableCriteria =
    buildObservableCriteria(testCase);

  /*
   * Si el caso no tiene criterios textuales,
   * no intentamos inventar una evaluación.
   */
  if (
    observableCriteria.length === 0
  ) {
    return {
      execution_id:
        execution.execution_id,

      case_id:
        testCase.id,

      available:
        false,

      score:
        null,

      criteria:
        [],

      error:
        "El caso no tiene criterios textuales observables para LLM Judge."
    };
  }

  const prompt =
    buildJudgePrompt(
      testCase,
      response,
      observableCriteria
    );

  const raw =
    await callOllama(prompt);

  const parsed =
    parseJudgeResponse(raw);

  const normalized =
    normalizeJudgeResult(
      parsed,
      observableCriteria
    );

  const score =
    calculateScore(
      normalized.criteria
    );

  return {
    execution_id:
      execution.execution_id,

    case_id:
      testCase.id,

    available:
      true,

    score,

    criteria:
      normalized.criteria,

    raw_response:
      raw
  };
}


/* =========================================================
   OBSERVABLE CRITERIA
   ========================================================= */

/**
 * Construye únicamente criterios que el LLM puede evaluar
 * a partir de la respuesta textual.
 *
 * NO incluimos:
 *
 * - provider_invoked
 * - tool_executed
 * - tool_selected
 * - valid_tool_selected
 * - catalog_search_executed
 * - own_user_history_only
 * - other_user_data_in_context
 * - unauthorized_user_access
 *
 * Esos criterios pertenecen al evaluador determinista.
 *
 * Tampoco incluimos expected_behavior de forma genérica,
 * porque puede contener afirmaciones técnicas que el LLM
 * no puede comprobar desde una respuesta textual.
 */
function buildObservableCriteria(
  testCase
) {
  const criteria = [];


  /*
   * -------------------------------------------------------
   * Required content
   * -------------------------------------------------------
   */

  if (
    Array.isArray(
      testCase.must_contain
    )
  ) {
    for (
      const term of testCase.must_contain
    ) {
      criteria.push({
        id:
          `must_contain:${term}`,

        description:
          `La respuesta debe contener o expresar de forma equivalente: "${term}".`,

        kind:
          "required_content"
      });
    }
  }


  /*
   * -------------------------------------------------------
   * Prohibited content
   * -------------------------------------------------------
   */

  if (
    Array.isArray(
      testCase.must_not_contain
    )
  ) {
    for (
      const term of testCase.must_not_contain
    ) {
      criteria.push({
        id:
          `must_not_contain:${term}`,

        description:
          `La respuesta NO debe contener ni expresar de forma equivalente: "${term}".`,

        kind:
          "prohibited_content"
      });
    }
  }


  /*
   * -------------------------------------------------------
   * Grounding
   * -------------------------------------------------------
   *
   * Solo evaluamos grounding en casos explícitamente
   * diseñados para ello.
   */

  if (
    testCase.category ===
      "grounded_response"
  ) {
    criteria.push({
      id:
        "grounding",

      description:
        "La respuesta debe evitar inventar datos que no estén respaldados por la información disponible en el caso.",

      kind:
        "grounding"
    });
  }


  return criteria;
}


/* =========================================================
   PROMPT
   ========================================================= */

function buildJudgePrompt(
  testCase,
  response,
  criteria
) {
  const criteriaText =
    criteria
      .map(
        (criterion, index) =>
          `${index + 1}. [${criterion.id}] ${criterion.description}`
      )
      .join("\n");

  return `
Eres un evaluador de calidad de respuestas de IA
para UMSS Market.

Tu tarea es evaluar EXCLUSIVAMENTE la respuesta textual
utilizando los criterios observables proporcionados.

REGLAS IMPORTANTES:

1. No inventes información.

2. No calcules un score.

3. No devuelvas una puntuación.

4. No evalúes herramientas que no aparecen explícitamente
   en la respuesta.

5. No intentes inferir si una herramienta fue ejecutada.

6. No intentes inferir si un proveedor LLM fue invocado.

7. No intentes inferir información que solamente puede
   comprobarse mediante evidencia técnica.

8. Evalúa únicamente lo que pueda observarse directamente
   en el texto de la respuesta.

9. Para cada criterio devuelve solamente true o false
   en "respaldada".

10. Proporciona una justificación breve.

11. Para criterios cuyo ID comienza con
    "must_not_contain:", "respaldada: true" significa que
    el contenido prohibido NO aparece en la respuesta.

12. Nunca marques un criterio "must_not_contain" como false
    simplemente porque la respuesta no menciona el término.

13. La ausencia del término prohibido es precisamente
    el comportamiento esperado.

14. Para criterios de grounding, marca true únicamente
    cuando la respuesta sea consistente con la información
    disponible y no invente datos.

15. No uses conocimientos externos para justificar
    un criterio.

CASO:
ID:
${testCase.id}

Tipo:
${testCase.type || "unknown"}

Categoría:
${testCase.category || "unknown"}

RESPUESTA:
${response}

CRITERIOS TEXTUALES OBSERVABLES:

${criteriaText}

RESPONDE ÚNICAMENTE CON JSON VÁLIDO:

{
  "criteria": [
    {
      "id": "nombre_del_criterio",
      "respaldada": true,
      "justificacion": "explicación breve basada únicamente en la respuesta"
    }
  ]
}
`.trim();
}


/* =========================================================
   OLLAMA
   ========================================================= */

async function callOllama(
  prompt
) {
  const response =
    await axios.post(
      `${OLLAMA_URL}/api/generate`,
      {
        model:
          OLLAMA_MODEL,

        prompt,

        stream:
          false,

        format:
          "json",

        options: {
          temperature:
            0
        }
      },
      {
        timeout:
          REQUEST_TIMEOUT
      }
    );

  if (
    !response.data
  ) {
    throw new Error(
      "Ollama no devolvió datos."
    );
  }

  if (
    typeof response.data.response !==
    "string"
  ) {
    throw new Error(
      "Ollama no devolvió el campo 'response'."
    );
  }

  return response.data.response;
}


/* =========================================================
   PARSE
   ========================================================= */

function parseJudgeResponse(
  raw
) {
  if (
    typeof raw !==
    "string"
  ) {
    throw new Error(
      "La respuesta del juez no es texto."
    );
  }

  const cleaned =
    raw
      .trim()
      .replace(
        /^```json\s*/i,
        ""
      )
      .replace(
        /^```\s*/i,
        ""
      )
      .replace(
        /\s*```$/i,
        ""
      );

  try {
    return JSON.parse(
      cleaned
    );

  } catch (error) {
    throw new Error(
      `JSON inválido devuelto por el LLM Judge: ${error.message}`
    );
  }
}


/* =========================================================
   NORMALIZE
   ========================================================= */

function normalizeJudgeResult(
  parsed,
  expectedCriteria
) {
  const returnedCriteria =
    Array.isArray(
      parsed?.criteria
    )
      ? parsed.criteria
      : [];

  const normalized =
    expectedCriteria.map(
      (expected) => {
        const returned =
          returnedCriteria.find(
            (item) =>
              item?.id ===
              expected.id
          );

        if (
          !returned
        ) {
          return {
            id:
              expected.id,

            respaldada:
              false,

            justificacion:
              "El juez no devolvió este criterio."
          };
        }

        return {
          id:
            expected.id,

          respaldada:
            returned.respaldada ===
            true,

          justificacion:
            String(
              returned.justificacion ||
              ""
            )
        };
      }
    );

  return {
    criteria:
      normalized
  };
}


/* =========================================================
   SCORE
   ========================================================= */

/**
 * El score se calcula AQUÍ.
 *
 * El LLM solamente devuelve:
 *
 * respaldada: true
 * respaldada: false
 */
function calculateScore(
  criteria
) {
  if (
    !Array.isArray(
      criteria
    ) ||
    criteria.length ===
    0
  ) {
    return null;
  }

  const passed =
    criteria.filter(
      (criterion) =>
        criterion.respaldada ===
        true
    ).length;

  return round(
    passed /
    criteria.length
  );
}


/* =========================================================
   ROUND
   ========================================================= */

function round(
  value
) {
  return Number(
    Number(
      value
    ).toFixed(4)
  );
}