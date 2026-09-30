/**
 * UMSS Market - Offline AI Evaluation Runner
 *
 * IMPORTANTE:
 * Este runner NO genera evidencia a partir de los valores esperados.
 *
 * El Golden Dataset define:
 *   - input
 *   - comportamiento esperado
 *   - criterios
 *
 * La evidencia observada debe provenir de fixtures/evidencias
 * independientes.
 *
 * QUALITY GATE:
 *   - 0 = PASA
 *   - 1 = NO PASA
 *
 * La compuerta verifica las métricas calculadas y además
 * garantiza que ningún caso crítico haya fallado.
 */

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

import { evaluateCriteria } from "../evaluators/criteriaEvaluator.js";

import {
  evaluateSecurity,
  calculateSecurityMetrics
} from "../evaluators/securityEvaluator.js";

import { judgeDataset } from "../evaluators/llmJudge.js";

import {
  calculateMetrics,
  formatMetricsSummary
} from "../metrics/metrics.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const EVALS_DIR = path.resolve(__dirname, "..");

const DATASET_PATH = path.join(
  EVALS_DIR,
  "dataset",
  "golden-dataset.json"
);

const THRESHOLDS_PATH = path.join(
  EVALS_DIR,
  "config",
  "thresholds.json"
);

const FIXTURES_PATH = path.join(
  EVALS_DIR,
  "fixtures",
  "observations.json"
);

const REPORTS_DIR = path.join(
  EVALS_DIR,
  "reports"
);

const REPORT_PATH = path.join(
  REPORTS_DIR,
  "offline-eval-report.json"
);

async function main() {
  console.log("");
  console.log("========================================");
  console.log("     UMSS MARKET - OFFLINE AI EVALS");
  console.log("========================================");
  console.log("");

  ensureReportsDirectory();

  const dataset = loadJson(DATASET_PATH);
  const thresholds = loadJson(THRESHOLDS_PATH);
  const observations = loadJson(FIXTURES_PATH);

  validateDataset(dataset);
  validateObservations(observations);

  console.log(`Dataset: ${dataset.dataset}`);
  console.log(`Versión: ${dataset.version}`);
  console.log(`Casos: ${dataset.cases.length}`);
  console.log(`Repeticiones: ${dataset.repetitions}`);
  console.log("");

  const executions = buildExecutions(
    dataset,
    observations
  );

  console.log(
    `Total de evaluaciones: ${executions.length}`
  );

  console.log("");

  console.log("Evaluando observaciones...");
  console.log("");

  const evaluatedExecutions = [];

  for (const execution of executions) {
    const criteriaResult = evaluateCriteria(
      execution.testCase,
      execution.response,
      execution.evidence
    );

    const securityResult = evaluateSecurity(
      execution.testCase,
      execution.evidence
    );

    evaluatedExecutions.push({
      ...execution,
      criteriaResult,
      securityResult
    });

    printCaseResult(
      execution,
      criteriaResult,
      securityResult
    );
  }

  const securityMetrics =
    calculateSecurityMetrics(
      evaluatedExecutions
    );

  /*
   * ---------------------------------------------------------
   * CASOS CRÍTICOS FALLADOS
   * ---------------------------------------------------------
   *
   * Un caso crítico falla si:
   *   - sus criterios no pasan, o
   *   - presenta una violación de seguridad.
   *
   * La consigna exige cero críticos fallados.
   */

  const criticalExecutions =
    evaluatedExecutions.filter(
      (execution) =>
        execution.testCase.critical === true
    );

  const failedCriticalExecutions =
    criticalExecutions.filter(
      (execution) =>
        execution.criteriaResult?.passed !== true ||
        execution.securityResult?.secure !== true
    );

  const criticalCasesTotal =
    criticalExecutions.length;

  const criticalCasesFailed =
    failedCriticalExecutions.length;

  console.log("");
  console.log("========================================");
  console.log("        CONTROL DE CASOS CRÍTICOS");
  console.log("========================================");
  console.log("");

  console.log(
    `Casos críticos: ${criticalCasesTotal}`
  );

  console.log(
    `Críticos fallados: ${criticalCasesFailed}`
  );

  if (criticalCasesFailed === 0) {
    console.log(
      "Estado críticos: PASS"
    );
  } else {
    console.log(
      "Estado críticos: FAIL"
    );

    for (
      const execution of failedCriticalExecutions
    ) {
      console.log(
        `  - ${execution.execution_id}`
      );
    }
  }

  console.log("");

  /*
   * ---------------------------------------------------------
   * LLM JUDGE
   * ---------------------------------------------------------
   */

  console.log("Ejecutando LLM Judge...");
  console.log("");

  let judgeResults;

  try {
    judgeResults = await judgeDataset(
      evaluatedExecutions
    );
  } catch (error) {
    console.warn(
      "⚠ LLM Judge no disponible."
    );

    console.warn(
      `Motivo: ${error.message}`
    );

    judgeResults = {
      available: false,
      error: error.message,
      results: []
    };
  }

  /*
   * ---------------------------------------------------------
   * MÉTRICAS
   * ---------------------------------------------------------
   */

  const metrics = calculateMetrics({
    executions: evaluatedExecutions,
    securityMetrics,
    judgeResults,
    thresholds
  });

  /*
   * ---------------------------------------------------------
   * QUALITY GATE
   * ---------------------------------------------------------
   *
   * calculateMetrics() determina las métricas generales.
   *
   * Aquí añadimos una condición adicional obligatoria:
   *
   *   criticalCasesFailed === 0
   *
   * Si alguna condición falla:
   *
   *   PASS -> false
   *   exit code -> 1
   *
   * Si todas pasan:
   *
   *   PASS -> true
   *   exit code -> 0
   */

  const calculatedGatePassed =
    metrics?.quality_gate?.passed === true;

  const criticalGatePassed =
    criticalCasesFailed === 0;

  const finalGatePassed =
    calculatedGatePassed &&
    criticalGatePassed;

  /*
   * Conservamos la información existente de
   * calculateMetrics() y añadimos los controles
   * específicos de esta compuerta.
   */

  metrics.quality_gate = {
    ...(metrics.quality_gate || {}),

    calculated_gate_passed:
      calculatedGatePassed,

    critical_cases_total:
      criticalCasesTotal,

    critical_cases_failed:
      criticalCasesFailed,

    critical_cases_passed:
      criticalGatePassed,

    passed:
      finalGatePassed
  };

  /*
   * ---------------------------------------------------------
   * REPORTE
   * ---------------------------------------------------------
   */

  const report = {
    metadata: {
      generated_at:
        new Date().toISOString(),

      dataset:
        dataset.dataset,

      version:
        dataset.version,

      domain:
        dataset.domain || "UMSS Market",

      repetitions:
        dataset.repetitions,

      total_cases:
        dataset.cases.length,

      total_evaluations:
        evaluatedExecutions.length,

      critical_cases:
        criticalCasesTotal,

      critical_cases_failed:
        criticalCasesFailed,

      evidence_source:
        "independent_observations_fixture"
    },

    thresholds,

    metrics,

    security: {
      metrics: securityMetrics
    },

    llm_judge:
      judgeResults,

    executions:
      evaluatedExecutions.map(
        sanitizeExecution
      )
  };

  saveJson(
    REPORT_PATH,
    report
  );

  /*
   * ---------------------------------------------------------
   * RESUMEN
   * ---------------------------------------------------------
   */

  console.log("");

  try {
    console.log(
      formatMetricsSummary(metrics)
    );
  } catch (error) {
    console.log(
      "Resumen de métricas:"
    );

    console.log(
      JSON.stringify(
        metrics,
        null,
        2
      )
    );
  }

  console.log("");

  console.log("========================================");
  console.log("             QUALITY GATE");
  console.log("========================================");
  console.log("");

  if (finalGatePassed) {
    console.log(
      "QUALITY GATE: PASS"
    );

    console.log(
      "Resultado: 0 = PASA"
    );
  } else {
    console.log(
      "QUALITY GATE: FAIL"
    );

    console.log(
      "Resultado: 1 = NO PASA"
    );
  }

  console.log("");

  console.log(
    `Métricas generales: ${
      calculatedGatePassed
        ? "PASS"
        : "FAIL"
    }`
  );

  console.log(
    `Casos críticos: ${
      criticalGatePassed
        ? "PASS"
        : "FAIL"
    }`
  );

  console.log("");

  console.log(
    `Reporte generado: ${REPORT_PATH}`
  );

  console.log("");

  console.log("========================================");
  console.log("     EVALUACIÓN FINALIZADA");
  console.log("========================================");
  console.log("");

  /*
   * ---------------------------------------------------------
   * CÓDIGO DE SALIDA
   * ---------------------------------------------------------
   *
   * ESTA PARTE ES IMPORTANTE.
   *
   * PASS -> 0
   * FAIL -> 1
   */

  process.exitCode =
    finalGatePassed
      ? 0
      : 1;
}

function buildExecutions(
  dataset,
  observations
) {
  const executions = [];

  const repetitions =
    Number(dataset.repetitions || 1);

  for (const testCase of dataset.cases) {
    const caseObservations =
      observations[testCase.id];

    if (!caseObservations) {
      throw new Error(
        `No existe observación independiente para ${testCase.id}.`
      );
    }

    for (
      let repetition = 1;
      repetition <= repetitions;
      repetition++
    ) {
      const observation =
        caseObservations[
          String(repetition)
        ];

      if (!observation) {
        throw new Error(
          `Falta observación para ${testCase.id}, repetición ${repetition}.`
        );
      }

      const input =
        resolveInput(
          testCase,
          observation
        );

      executions.push({
        execution_id:
          `${testCase.id}-RUN-${String(
            repetition
          ).padStart(2, "0")}`,

        repetition,

        testCase,

        input,

        response:
          String(
            observation.response || ""
          ),

        evidence:
          observation.evidence || {}
      });
    }
  }

  return executions;
}

function resolveInput(
  testCase,
  observation
) {
  if (
    typeof observation.input ===
    "string"
  ) {
    return observation.input;
  }

  if (
    typeof testCase.input ===
    "string"
  ) {
    return testCase.input;
  }

  if (
    testCase.input_generator?.type ===
    "repeat"
  ) {
    const value =
      String(
        testCase.input_generator.value ||
        ""
      );

    const count =
      Number(
        testCase.input_generator.count ||
        0
      );

    return value.repeat(count);
  }

  return "";
}

function printCaseResult(
  execution,
  criteriaResult,
  securityResult
) {
  const criteriaStatus =
    criteriaResult?.passed === true
      ? "PASS"
      : "FAIL";

  const securityStatus =
    securityResult?.secure === true
      ? "SECURE"
      : "VIOLATION";

  console.log(
    `${execution.execution_id} | ` +
    `${criteriaStatus} | ` +
    `${securityStatus}`
  );
}

function sanitizeExecution(
  execution
) {
  return {
    execution_id:
      execution.execution_id,

    repetition:
      execution.repetition,

    case_id:
      execution.testCase.id,

    attack_id:
      execution.testCase.attack_id ||
      null,

    type:
      execution.testCase.type ||
      null,

    title:
      execution.testCase.title ||
      null,

    category:
      execution.testCase.category ||
      null,

    critical:
      execution.testCase.critical === true,

    input_length:
      execution.input.length,

    evidence:
      execution.evidence,

    criteria:
      execution.criteriaResult,

    security:
      execution.securityResult,

    response:
      execution.response
  };
}

function validateDataset(
  dataset
) {
  if (!dataset) {
    throw new Error(
      "El Golden Dataset está vacío."
    );
  }

  if (
    !Array.isArray(
      dataset.cases
    )
  ) {
    throw new Error(
      "El Golden Dataset debe contener 'cases'."
    );
  }

  /*
   * Se conserva la validación actual
   * del repositorio: mínimo 15 casos.
   *
   * No modificamos todavía esta parte porque
   * primero debemos resolver la discrepancia
   * entre los 12 casos de la consigna y los
   * 15 casos actualmente implementados.
   */

  if (
    dataset.cases.length <
    15
  ) {
    throw new Error(
      `El Golden Dataset tiene ${dataset.cases.length} casos. Se requieren al menos 15.`
    );
  }

  /*
   * Se conserva el mínimo actual del repositorio.
   */

  const criticalCases =
    dataset.cases.filter(
      (testCase) =>
        testCase.critical === true
    );

  if (
    criticalCases.length <
    4
  ) {
    throw new Error(
      `El Golden Dataset tiene ${criticalCases.length} casos críticos. Se requieren al menos 4.`
    );
  }

  const ids =
    new Set();

  for (
    const testCase of dataset.cases
  ) {
    if (!testCase.id) {
      throw new Error(
        "Existe un caso sin id."
      );
    }

    if (
      ids.has(testCase.id)
    ) {
      throw new Error(
        `ID duplicado en dataset: ${testCase.id}`
      );
    }

    ids.add(testCase.id);

    if (
      !Array.isArray(
        testCase.criteria
      )
    ) {
      throw new Error(
        `El caso ${testCase.id} no tiene criteria.`
      );
    }
  }
}

function validateObservations(
  observations
) {
  if (
    !observations ||
    typeof observations !==
      "object"
  ) {
    throw new Error(
      "El archivo de observaciones está vacío o no es válido."
    );
  }
}

function loadJson(
  filePath
) {
  if (
    !fs.existsSync(
      filePath
    )
  ) {
    throw new Error(
      `No existe el archivo requerido: ${filePath}`
    );
  }

  const content =
    fs.readFileSync(
      filePath,
      "utf8"
    );

  try {
    return JSON.parse(
      content
    );
  } catch (error) {
    throw new Error(
      `JSON inválido en ${filePath}: ${error.message}`
    );
  }
}

function saveJson(
  filePath,
  data
) {
  fs.writeFileSync(
    filePath,
    JSON.stringify(
      data,
      null,
      2
    ),
    "utf8"
  );
}

function ensureReportsDirectory() {
  fs.mkdirSync(
    REPORTS_DIR,
    {
      recursive: true
    }
  );
}

main().catch(
  (error) => {
    console.error("");
    console.error(
      "========================================"
    );
    console.error(
      "ERROR EN OFFLINE AI EVALS"
    );
    console.error(
      "========================================"
    );
    console.error("");
    console.error(
      error.message
    );
    console.error("");

    if (error.stack) {
      console.error(
        error.stack
      );
    }

    console.error("");

    /*
     * Un error de ejecución también debe
     * considerarse NO PASA.
     */
    process.exitCode = 1;
  }
);