import fs from "fs";
import path from "path";
import axios from "axios";
import dotenv from "dotenv";

dotenv.config();

const BASE_DIR = process.cwd();

const INPUT =
  path.join(
    BASE_DIR,
    "evals",
    "calibration",
    "v2-calibration.json"
  );

const OUTPUT =
  path.join(
    BASE_DIR,
    "evals",
    "calibration",
    "v2-model-judge-comparison.json"
  );

const OLLAMA_URL =
  process.env.OLLAMA_URL ||
  "http://localhost:11434";

const OLLAMA_MODEL =
  process.env.OLLAMA_MODEL ||
  "qwen2.5:7b";

const TIMEOUT =
  Number(
    process.env.LLM_JUDGE_TIMEOUT ||
    30000
  );


function round(value) {
  return Number(
    Number(value).toFixed(4)
  );
}


function buildPrompt(item) {
  const evidence =
    JSON.stringify(
      item.evidence || {},
      null,
      2
    );

  return `
Eres un evaluador de calidad de IA para UMSS Market.

Evalúa únicamente si el comportamiento observado
corresponde al resultado esperado indicado en el caso.

No inventes información.
No calcules un score.
Devuelve únicamente true o false.

CASO:
${item.case_id}

DESCRIPCIÓN:
${item.description || ""}

EVIDENCIA:
${evidence}

RESULTADO ESPERADO:
${item.expected_behavior || ""}

CRITERIO:
La evaluación debe ser TRUE únicamente si la evidencia
demuestra que el comportamiento esperado se cumple.

Responde únicamente JSON válido:

{
  "respaldada": true,
  "justificacion": "explicación breve basada en la evidencia"
}
`.trim();
}


async function askJudge(item) {
  const prompt =
    buildPrompt(item);

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
          temperature: 0
        }
      },
      {
        timeout:
          TIMEOUT
      }
    );

  const raw =
    response?.data?.response;

  if (
    typeof raw !== "string"
  ) {
    throw new Error(
      "Ollama no devolvió response."
    );
  }

  return JSON.parse(
    raw.trim()
  );
}


function calculateAgreement(
  rows
) {
  const valid =
    rows.filter(
      (row) =>
        typeof row.model_label ===
          "number" &&
        typeof row.human_label ===
          "number"
    );

  if (valid.length === 0) {
    return {
      agreement: null,
      kappa: null,
      tp: 0,
      tn: 0,
      fp: 0,
      fn: 0
    };
  }

  let tp = 0;
  let tn = 0;
  let fp = 0;
  let fn = 0;

  for (const row of valid) {
    if (
      row.human_label === 1 &&
      row.model_label === 1
    ) {
      tp++;
    } else if (
      row.human_label === 0 &&
      row.model_label === 0
    ) {
      tn++;
    } else if (
      row.human_label === 0 &&
      row.model_label === 1
    ) {
      fp++;
    } else if (
      row.human_label === 1 &&
      row.model_label === 0
    ) {
      fn++;
    }
  }

  const total =
    valid.length;

  const agreement =
    (tp + tn) /
    total;

  const pYesHuman =
    (tp + fn) /
    total;

  const pYesModel =
    (tp + fp) /
    total;

  const pNoHuman =
    (tn + fp) /
    total;

  const pNoModel =
    (tn + fn) /
    total;

  const expectedAgreement =
    pYesHuman * pYesModel +
    pNoHuman * pNoModel;

  const kappa =
    expectedAgreement === 1
      ? 1
      : (
          (agreement -
            expectedAgreement) /
          (1 -
            expectedAgreement)
        );

  return {
    agreement:
      round(agreement),

    kappa:
      round(kappa),

    tp,
    tn,
    fp,
    fn
  };
}


async function main() {
  if (
    !fs.existsSync(INPUT)
  ) {
    throw new Error(
      `No existe: ${INPUT}`
    );
  }

  const dataset =
    JSON.parse(
      fs.readFileSync(
        INPUT,
        "utf8"
      )
    );

  const cases =
    Array.isArray(dataset)
      ? dataset
      : dataset.cases;

  if (
    !Array.isArray(cases)
  ) {
    throw new Error(
      "El archivo de calibración no contiene un arreglo de casos."
    );
  }

  console.log("");
  console.log(
    "UMSS MARKET - CALIBRACIÓN V2 CON LLM JUDGE"
  );
  console.log(
    `Modelo: ${OLLAMA_MODEL}`
  );
  console.log(
    `Casos: ${cases.length}`
  );
  console.log("");

  const rows = [];

  for (
    const item of cases
  ) {
    process.stdout.write(
      `${item.case_id} ... `
    );

    try {
      const judge =
        await askJudge(item);

      const modelLabel =
        judge.respaldada === true
          ? 1
          : 0;

      const humanLabel =
        Number(
          item.human_label
        );

      const agreement =
        modelLabel ===
        humanLabel;

      rows.push({
        case_id:
          item.case_id,

        base_case:
          item.base_case,

        human_label:
          humanLabel,

        model_label:
          modelLabel,

        agreement,

        justificacion:
          String(
            judge.justificacion ||
            ""
          )
      });

      console.log(
        agreement
          ? "MATCH"
          : "MISMATCH"
      );

    } catch (error) {
      rows.push({
        case_id:
          item.case_id,

        base_case:
          item.base_case,

        human_label:
          Number(
            item.human_label
          ),

        model_label:
          null,

        agreement:
          false,

        error:
          error.message
      });

      console.log(
        `ERROR: ${error.message}`
      );
    }
  }

  const metrics =
    calculateAgreement(
      rows
    );

  const output = {
    calibration_version:
      "V2",

    evaluator:
      "LLM Judge",

    model:
      OLLAMA_MODEL,

    dataset:
      "v1-calibration-cases.json",

    cases:
      rows.length,

    valid_cases:
      rows.filter(
        (row) =>
          row.model_label !==
          null
      ).length,

    agreement:
      metrics.agreement,

    cohens_kappa:
      metrics.kappa,

    confusion_matrix: {
      tp:
        metrics.tp,

      tn:
        metrics.tn,

      fp:
        metrics.fp,

      fn:
        metrics.fn
    },

    results:
      rows,

    generated_at:
      new Date().toISOString()
  };

  fs.writeFileSync(
    OUTPUT,
    JSON.stringify(
      output,
      null,
      2
    ),
    "utf8"
  );

  console.log("");
  console.log(
    "RESULTADO CALIBRACIÓN V2"
  );
  console.log(
    `Agreement: ${metrics.agreement}`
  );
  console.log(
    `Cohen's Kappa: ${metrics.kappa}`
  );
  console.log("");
  console.log(
    "Confusion:"
  );
  console.log(
    `TP ${metrics.tp}`
  );
  console.log(
    `TN ${metrics.tn}`
  );
  console.log(
    `FP ${metrics.fp}`
  );
  console.log(
    `FN ${metrics.fn}`
  );
  console.log("");
  console.log(
    `Reporte: ${OUTPUT}`
  );
}


main().catch(
  (error) => {
    console.error("");
    console.error(
      "ERROR:",
      error.message
    );
    process.exit(1);
  }
);