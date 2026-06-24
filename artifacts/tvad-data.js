window.TVAD_DATA = {
  meta: { name: "Tiwa's Vector Allocation Dynamics", short: "TVAD", scaleMin: 1, scaleMax: 5 },
  axes: [
    {
      id: "IMV",
      name: "Information Metrology Vector",
      tension: "Resolution vs. Scope",
      description: "Measures the cognitive filter through which raw reality is captured and converted into actionable data points.",
      poleA: {
        id: "De",
        name: "Data-Empirical",
        subtraits: [
          {
            id: "Sr",
            name: "Sensory Resolution",
            description: "Granular precision of immediate physical data capture. High Sr detects minute changes; low Sr filters noise.",
            prompt: "I immediately notice small physical changes and details in my environment that others miss."
          },
          {
            id: "Mr",
            name: "Metric Retention",
            description: "Raw storage of exact numerical, factual, and historical values for later recall.",
            prompt: "I can recall exact numbers, facts, and historical details long after I first encountered them."
          },
          {
            id: "Ac",
            name: "Actuarial Calibration",
            description: "Spotting localized anomalies and statistical deviations against an expected baseline.",
            prompt: "I quickly spot when a single data point or result deviates from what is statistically normal."
          }
        ]
      },
      poleB: {
        id: "Sa",
        name: "Structural-Abstract",
        subtraits: [
          {
            id: "Px",
            name: "Pattern Extrapolation",
            description: "Projecting future trends and outcomes from incomplete or partial data.",
            prompt: "I can confidently project future trends from incomplete information."
          },
          {
            id: "Sm",
            name: "Systemic Mapping",
            description: "Manipulating complex multi-node theoretical structures in working memory.",
            prompt: "I can hold and manipulate complex, multi-part theoretical systems in my head at once."
          },
          {
            id: "Cs",
            name: "Conceptual Synthesis",
            description: "Merging disparate theories and frameworks into a single coherent model.",
            prompt: "I enjoy merging unrelated theories and ideas into one unified model."
          }
        ]
      }
    },
    {
      id: "EAV",
      name: "Energetic Allocation Vector",
      tension: "Throughput vs. Efficiency",
      description: "Measures the default operational strategy for managing metabolic and cognitive energy.",
      poleA: {
        id: "Ke",
        name: "Kinetic-Expansive",
        subtraits: [
          {
            id: "Vt",
            name: "Volition Threshold",
            description: "Raw burst-energy available to force a state change or overcome friction.",
            prompt: "I can summon a burst of raw energy to push through obstacles and force things to change."
          },
          {
            id: "Va",
            name: "Variance Tolerance",
            description: "Capacity to function optimally amid high instability and unpredictability.",
            prompt: "I function at my best in highly unstable, unpredictable situations."
          },
          {
            id: "Re",
            name: "Resource Expenditure Rate",
            description: "Willingness to rapidly burn capital for immediate high-leverage gains.",
            prompt: "I am willing to rapidly spend resources for an immediate, high-leverage payoff."
          }
        ]
      },
      poleB: {
        id: "Hp",
        name: "Homeostatic-Preservative",
        subtraits: [
          {
            id: "Tm",
            name: "Threat Mitigation",
            description: "Continuous identification and neutralization of environmental vulnerabilities.",
            prompt: "I constantly scan for and neutralize potential threats and vulnerabilities around me."
          },
          {
            id: "Ss",
            name: "Systemic Stabilization",
            description: "Enforcing order, routine, and predictability onto chaos.",
            prompt: "I instinctively impose order and routine to make chaotic situations predictable."
          },
          {
            id: "Cc",
            name: "Cognitive Conservation",
            description: "Automating routines to minimize energy drain.",
            prompt: "I prefer to automate and routinize tasks so they drain as little of my energy as possible."
          }
        ]
      }
    },
    {
      id: "VOV",
      name: "Valuative Optimization Vector",
      tension: "Transactional Utility vs. Relational Equilibrium",
      description: "Measures the metric used to calculate the success, failure, and yield of a decision.",
      poleA: {
        id: "Os",
        name: "Objective-Systemic",
        subtraits: [
          {
            id: "Tu",
            name: "Transactional Utility",
            description: "Maximizing the measurable material output of an interaction.",
            prompt: "I judge an interaction mainly by how much measurable material value it produces."
          },
          {
            id: "Fa",
            name: "Structural Friction Analysis",
            description: "Detecting logical inefficiencies and bottlenecks within a system.",
            prompt: "I quickly detect the logical inefficiencies and bottlenecks in any system."
          },
          {
            id: "Dx",
            name: "Detached Execution",
            description: "Firewalling decisions from emotional and social pressure.",
            prompt: "I can make decisions while completely insulating them from emotional or social pressure."
          }
        ]
      },
      poleB: {
        id: "Rc",
        name: "Relational-Cohesive",
        subtraits: [
          {
            id: "Ar",
            name: "Affective Resonance",
            description: "Reading and mirroring others' emotional states in real time.",
            prompt: "I naturally read and mirror the emotional states of people around me in real time."
          },
          {
            id: "Nc",
            name: "Network Cohesion",
            description: "Maintaining group alignment, mitigating conflict, and building trust.",
            prompt: "I work to keep groups aligned, defuse conflict, and build trust between people."
          },
          {
            id: "Sc",
            name: "Social Capital Allocation",
            description: "Strategic long-term investment in interpersonal equity.",
            prompt: "I strategically invest in relationships to build long-term interpersonal equity."
          }
        ]
      }
    },
    {
      id: "VLV",
      name: "Volitional Latency Vector",
      tension: "Agility vs. Trajectory",
      description: "Measures the execution mechanic of behavior when interacting with timelines and variable environments.",
      poleA: {
        id: "Ra",
        name: "Reactive-Adaptive",
        subtraits: [
          {
            id: "Fi",
            name: "Feedback Integration Speed",
            description: "Rapidly altering behavior based on incoming real-time feedback.",
            prompt: "I rapidly change my behavior the moment I receive new real-time feedback."
          },
          {
            id: "To",
            name: "Tactical Opportunism",
            description: "Instantly exploiting sudden, unplanned advantages.",
            prompt: "I instantly seize sudden, unplanned opportunities when they appear."
          },
          {
            id: "Sa",
            name: "Sunk-Cost Abandonment",
            description: "Instantly discarding failing strategies without emotional drag.",
            prompt: "I can drop a failing plan immediately, without being held back by what I already invested."
          }
        ]
      },
      poleB: {
        id: "Dl",
        name: "Deterministic-Linear",
        subtraits: [
          {
            id: "Sp",
            name: "Sequential Processing",
            description: "Step-by-step methodical execution, refusing to skip steps.",
            prompt: "I work through tasks step by step and refuse to skip any part of the sequence."
          },
          {
            id: "Tc",
            name: "Trajectory Commitment",
            description: "Resisting distraction and deviation once a target is locked.",
            prompt: "Once I lock onto a goal, I resist any distraction or deviation from it."
          },
          {
            id: "Dy",
            name: "Delayed Yield Tolerance",
            description: "Sustaining high-effort work without immediate reward.",
            prompt: "I can sustain high-effort work for a long time even with no immediate reward."
          }
        ]
      }
    }
  ]
};
