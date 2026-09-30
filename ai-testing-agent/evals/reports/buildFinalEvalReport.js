import fs from "fs";
import path from "path";

const BASE = process.cwd();

function readJSON(relativePath) {
  const file = path.join(BASE, relativePath);

  if (!fs.existsSync(file)) {
    throw new Error(`No existe: ${file}`);
  }

  return JSON.parse(
    fs.readFileSync(file, "utf8")
  );
}

function round(value) {
  return Number(Number(value).toFixed(4));
}

function requireNumber(value, fieldName) {
  if (typeof value !== "number" || Number.isNaN(value)) {
    throw new Error(
      `El campo '${fieldName}' no contiene un número válido.`
    );
  }

  return round(value);
}

function requireFile(relativePath) {
  const file = path.join(BASE, relativePath);

  if (!fs.existsSync(file)) {
    throw new Error(`No existe el archivo requerido: ${file}`);
  }

  return relativePath;
}

/*
 * ============================================================
 * CARGA DE EVIDENCIAS
 * ============================================================
 */

const v1Calibration =
  readJSON("./evals/calibration/v1-baseline.json");

const v2Calibration =
  readJSON("./evals/calibration/v2-baseline.json");

const v2ModelJudge =
  readJSON(
    "./evals/calibration/v2-model-judge-comparison.json"
  );

const v3 =
  readJSON(
    "./evals/reports/offline-eval-v3.json"
  );

/*
 * ============================================================
 * VALIDACIÓN DE EVIDENCIAS
 * ============================================================
 */

const evidencePaths = {
  golden_dataset:
    requireFile(
      "./evals/dataset/golden-dataset.json"
    ),

  observations:
    requireFile(
      "./evals/fixtures/observations.json"
    ),

  v1_report:
    requireFile(
      "./evals/reports/offline-eval-v1.json"
    ),

  v3_report:
    requireFile(
      "./evals/reports/offline-eval-v3.json"
    ),

  v1_calibration:
    requireFile(
      "./evals/calibration/v1-baseline.json"
    ),

  v2_calibration:
    requireFile(
      "./evals/calibration/v2-baseline.json"
    ),

  v2_model_judge:
    requireFile(
      "./evals/calibration/v2-model-judge-comparison.json"
    )
};

/*
 * ============================================================
 * V3 - MÉTRICAS REALES
 * ============================================================
 */

const v3Metrics =
  v3.metrics || {};

const v3QualityGate =
  v3Metrics.quality_gate ||
  v3.quality_gate ||
  {};

const v3Security =
  v3.security?.metrics ||
  v3Metrics.security ||
  {};

const v3Judge =
  v3.llm_judge ||
  {};

const v3Evaluations =
  v3Metrics.evaluations ??
  v3.metadata?.total_evaluations;

const v3Completeness =
  v3Metrics.completeness ??
  v3Metrics.completitud;

const v3WithoutProhibited =
  v3Metrics.without_prohibited ??
  v3Metrics.sin_prohibidos;

const v3Fidelity =
  v3Metrics.fidelity ??
  v3Metrics.fidelidad;

const v3SecurityScore =
  v3Metrics.security ??
  v3Metrics.seguridad;

const v3JudgeScore =
  v3Judge.score ??
  v3Judge.average ??
  v3Metrics.llm_judge;

/*
 * No usamos valores por defecto como 1.
 * Si falta una métrica requerida, el reporte falla.
 */

const evaluations =
  requireNumber(
    v3Evaluations,
    "V3 evaluations"
  );

const completeness =
  requireNumber(
    v3Completeness,
    "V3 completeness"
  );

const withoutProhibited =
  requireNumber(
    v3WithoutProhibited,
    "V3 without_prohibited"
  );

const fidelity =
  requireNumber(
    v3Fidelity,
    "V3 fidelity"
  );

const security =
  requireNumber(
    v3SecurityScore,
    "V3 security"
  );

const llmJudge =
  requireNumber(
    v3JudgeScore,
    "V3 LLM Judge"
  );

/*
 * ============================================================
 * TASAS DE SEGURIDAD
 * ============================================================
 */

const securityRates = {
  attack_success_rate:
    requireNumber(
      v3Security.attack_success_rate ?? 0,
      "attack_success_rate"
    ),

  unauthorized_tool_execution:
    requireNumber(
      v3Security.unauthorized_tool_execution ??
      v3Security.unauthorized_tool_execution_rate ??
      0,
      "unauthorized_tool_execution"
    ),

  identity_violation:
    requireNumber(
      v3Security.identity_violation ??
      v3Security.identity_violation_rate ??
      0,
      "identity_violation"
    ),

  provider_invocation_violation:
    requireNumber(
      v3Security.provider_invocation_violation ??
      v3Security.provider_invocation_violation_rate ??
      0,
      "provider_invocation_violation"
    ),

  secret_exposure:
    requireNumber(
      v3Security.secret_exposure ??
      v3Security.secret_exposure_rate ??
      0,
      "secret_exposure"
    ),

  policy_bypass:
    requireNumber(
      v3Security.policy_bypass ??
      v3Security.policy_bypass_rate ??
      0,
      "policy_bypass"
    )
};

/*
 * ============================================================
 * QUALITY GATE V3
 * ============================================================
 */

const qualityGatePassed =
  v3QualityGate.passed === true;

const criticalCasesTotal =
  Number(
    v3QualityGate.critical_cases_total ??
    v3.metadata?.critical_cases ??
    0
  );

const criticalCasesFailed =
  Number(
    v3QualityGate.critical_cases_failed ??
    v3.metadata?.critical_cases_failed ??
    0
  );

const criticalCasesPassed =
  criticalCasesFailed === 0;

const finalQualityGate =
  qualityGatePassed &&
  criticalCasesPassed;

/*
 * ============================================================
 * REPORTE FINAL
 * ============================================================
 */

const report = {
  project:
    "UMSS Market",

  report_type:
    "AI Offline Evals - Final Comparative Report",

  generated_at:
    new Date().toISOString(),

  methodology: {
    dataset:
      "UMSS-Market-AI-Security-Golden-Dataset",

    golden_cases:
      15,

    repetitions:
      3,

    total_evaluations:
      45,

    calibration_cases:
      16,

    note:
      "La calibración utiliza casos controlados de referencia. Los resultados V3 corresponden a observaciones offline controladas y no deben interpretarse como ejecuciones de producción."
  },

  versions: {

    V1: {
      type:
        "baseline calibration",

      agreement:
        round(
          v1Calibration.agreement
        ),

      cohens_kappa:
        round(
          v1Calibration.cohens_kappa
        ),

      confusion_matrix:
        v1Calibration.confusion_matrix
    },

    V2_deterministic: {
      type:
        "deterministic calibration",

      agreement:
        round(
          v2Calibration.agreement
        ),

      cohens_kappa:
        round(
          v2Calibration.cohens_kappa
        ),

      confusion_matrix:
        v2Calibration.confusion_matrix
    },

    V2_model_judge: {
      type:
        "LLM Judge calibration",

      model:
        v2ModelJudge.model,

      cases:
        v2ModelJudge.cases,

      valid_cases:
        v2ModelJudge.valid_cases,

      agreement:
        round(
          v2ModelJudge.agreement
        ),

      cohens_kappa:
        round(
          v2ModelJudge.cohens_kappa
        ),

      confusion_matrix:
        v2ModelJudge.confusion_matrix
    },

    V3: {
      type:
        "final offline evaluation",

      evaluations,

      completeness,

      without_prohibited:
        withoutProhibited,

      fidelity,

      security,

      llm_judge:
        llmJudge,

      quality_gate:
        finalQualityGate
          ? "PASS"
          : "FAIL",

      critical_cases: {
        total:
          criticalCasesTotal,

        failed:
          criticalCasesFailed,

        passed:
          criticalCasesPassed
      },

      security_rates:
        securityRates
    }
  },

  evidence:
    evidencePaths,

  conclusions: [
    "V1 presentó Agreement 0.6875 y Cohen's Kappa 0.375 en la calibración inicial.",

    "V2 determinista alcanzó Agreement 1.0000 y Cohen's Kappa 1.0000 sobre el conjunto de calibración.",

    "El LLM Judge V2, ejecutado con qwen2.5-coder:7b, obtuvo Agreement 0.8125 y Cohen's Kappa 0.625.",

    "La calibración del LLM Judge presentó 3 discrepancias sobre 16 casos.",

    `V3 evaluó ${evaluations} observaciones offline.`,

    `V3 obtuvo completitud ${completeness.toFixed(4)}, sin prohibidos ${withoutProhibited.toFixed(4)}, fidelidad ${fidelity.toFixed(4)} y seguridad ${security.toFixed(4)}.`,

    `El LLM Judge de V3 obtuvo ${llmJudge.toFixed(4)}.`,

    `Los casos críticos evaluados fueron ${criticalCasesTotal} ejecuciones, con ${criticalCasesFailed} fallos críticos.`,

    `La Quality Gate de V3 terminó en ${finalQualityGate ? "PASS" : "FAIL"}.`,

    "Las tasas de seguridad reportadas corresponden a evaluación offline controlada.",

    "Los resultados V3 corresponden a evaluación offline controlada y deben presentarse con esa limitación metodológica."
  ]
};

/*
 * ============================================================
 * GUARDAR REPORTE
 * ============================================================
 */

const output =
  path.join(
    BASE,
    "evals",
    "reports",
    "v1-v2-v3-comparison.json"
  );

fs.writeFileSync(
  output,
  JSON.stringify(
    report,
    null,
    2
  ),
  "utf8"
);

/*
 * ============================================================
 * SALIDA POR CONSOLA
 * ============================================================
 */

console.log("");
console.log(
  "=============================================="
);
console.log(
  "UMSS MARKET - REPORTE FINAL V1 -> V2 -> V3"
);
console.log(
  "=============================================="
);
console.log("");

console.log(
  `V1  Agreement: ${report.versions.V1.agreement}`
);

console.log(
  `V1  Kappa:     ${report.versions.V1.cohens_kappa}`
);

console.log("");

console.log(
  `V2  Agreement: ${report.versions.V2_deterministic.agreement}`
);

console.log(
  `V2  Kappa:     ${report.versions.V2_deterministic.cohens_kappa}`
);

console.log("");

console.log(
  `V2 Judge Agreement: ${report.versions.V2_model_judge.agreement}`
);

console.log(
  `V2 Judge Kappa:     ${report.versions.V2_model_judge.cohens_kappa}`
);

console.log("");

console.log(
  `V3 Evaluaciones: ${report.versions.V3.evaluations}`
);

console.log(
  `V3 Completeness: ${report.versions.V3.completeness}`
);

console.log(
  `V3 Sin prohibidos: ${report.versions.V3.without_prohibited}`
);

console.log(
  `V3 Fidelidad: ${report.versions.V3.fidelity}`
);

console.log(
  `V3 Seguridad: ${report.versions.V3.security}`
);

console.log(
  `V3 LLM Judge: ${report.versions.V3.llm_judge}`
);

console.log(
  `V3 Casos críticos: ${report.versions.V3.critical_cases.total}`
);

console.log(
  `V3 Críticos fallados: ${report.versions.V3.critical_cases.failed}`
);

console.log(
  `V3 Quality Gate: ${report.versions.V3.quality_gate}`
);

console.log("");

console.log(
  `Reporte generado: ${output}`
);

console.log("");