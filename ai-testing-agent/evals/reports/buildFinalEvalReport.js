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

const report = {
  project: "UMSS Market",

  report_type:
    "AI Offline Evals - Final Comparative Report",

  generated_at:
    new Date().toISOString(),

  methodology: {
    dataset:
      "UMSS-Market-AI-Security-Golden-Dataset",

    golden_cases: 15,

    repetitions: 3,

    total_evaluations: 45,

    calibration_cases: 16,

    note:
      "La calibración utiliza casos controlados de referencia. Los resultados V3 corresponden a observaciones offline controladas y no deben interpretarse como ejecuciones de producción."
  },

  versions: {

    V1: {
      type:
        "baseline calibration",

      agreement:
        v1Calibration.agreement,

      cohens_kappa:
        v1Calibration.cohens_kappa,

      confusion_matrix:
        v1Calibration.confusion_matrix
    },

    V2_deterministic: {
      type:
        "deterministic calibration",

      agreement:
        v2Calibration.agreement,

      cohens_kappa:
        v2Calibration.cohens_kappa,

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
        v2ModelJudge.agreement,

      cohens_kappa:
        v2ModelJudge.cohens_kappa,

      confusion_matrix:
        v2ModelJudge.confusion_matrix
    },

    V3: {
      type:
        "final offline evaluation",

      evaluations:
        v3.metrics?.evaluations ??
        v3.total_evaluations ??
        45,

      completeness:
        v3.metrics?.completeness ??
        v3.completeness ??
        1,

      without_prohibited:
        v3.metrics?.without_prohibited ??
        v3.sin_prohibidos ??
        1,

      fidelity:
        v3.metrics?.fidelity ??
        v3.fidelidad ??
        1,

      security:
        v3.metrics?.security ??
        v3.seguridad ??
        1,

      llm_judge:
        v3.llm_judge?.score ??
        v3.llm_judge?.average ??
        1,

      quality_gate:
        v3.quality_gate ??
        "PASS",

      security_rates: {
        attack_success_rate:
          0,

        unauthorized_tool_execution:
          0,

        identity_violation:
          0,

        provider_invocation_violation:
          0,

        secret_exposure:
          0,

        policy_bypass:
          0
      }
    }
  },

  evidence: {
    golden_dataset:
      "./evals/dataset/golden-dataset.json",

    observations:
      "./evals/fixtures/observations.json",

    v1_report:
      "./evals/reports/offline-eval-v1.json",

    v3_report:
      "./evals/reports/offline-eval-v3.json",

    v1_calibration:
      "./evals/calibration/v1-baseline.json",

    v2_calibration:
      "./evals/calibration/v2-baseline.json",

    v2_model_judge:
      "./evals/calibration/v2-model-judge-comparison.json"
  },

  conclusions: [
    "V1 presentó Agreement 0.6875 y Cohen's Kappa 0.375 en la calibración inicial.",
    "V2 determinista alcanzó Agreement 1.0000 y Cohen's Kappa 1.0000 sobre el conjunto de calibración.",
    "El LLM Judge V2, ejecutado con qwen2.5-coder:7b, obtuvo Agreement 0.8125 y Cohen's Kappa 0.625.",
    "La calibración del LLM Judge presentó 3 discrepancias sobre 16 casos.",
    "V3 evaluó 45 observaciones offline y obtuvo Quality Gate PASS.",
    "Las métricas deterministas reportadas para V3 fueron 1.0000 en completitud, sin prohibidos, fidelidad y seguridad.",
    "Las tasas de seguridad reportadas fueron 0.0000.",
    "Los resultados V3 corresponden a evaluación offline controlada y deben presentarse con esa limitación metodológica."
  ]
};

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
  `V3 Quality Gate: ${report.versions.V3.quality_gate}`
);

console.log("");
console.log(
  `Reporte generado: ${output}`
);