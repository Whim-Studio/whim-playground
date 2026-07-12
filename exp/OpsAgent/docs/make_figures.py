"""Generate architecture / flow / DFD / UML diagrams for the OpsAgent
college project proposal and report. Pure matplotlib, no external services."""
import os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Rectangle, Circle
from matplotlib.lines import Line2D

OUT = os.path.join(os.path.dirname(__file__), "figures")
os.makedirs(OUT, exist_ok=True)

# Palette (colour-blind safe, print friendly)
C = {
    "blue":   "#2563eb",
    "cyan":   "#0891b2",
    "teal":   "#0d9488",
    "amber":  "#b45309",
    "red":    "#b91c1c",
    "green":  "#15803d",
    "slate":  "#334155",
    "violet": "#6d28d9",
    "bg":     "#f1f5f9",
    "line":   "#475569",
}
plt.rcParams["font.family"] = "DejaVu Sans"


def box(ax, x, y, w, h, text, fc, tc="white", fs=10, style="round,pad=0.02", ec=None):
    p = FancyBboxPatch((x, y), w, h, boxstyle=style,
                       linewidth=1.4, edgecolor=ec or fc, facecolor=fc, zorder=3)
    ax.add_patch(p)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center",
            fontsize=fs, color=tc, weight="bold", zorder=4, wrap=True)
    return (x + w / 2, y + h / 2, w, h)


def arrow(ax, p1, p2, color=C["line"], style="-|>", lw=1.6, rad=0.0, ls="-"):
    a = FancyArrowPatch(p1, p2, arrowstyle=style, mutation_scale=14,
                        linewidth=lw, color=color, zorder=2,
                        connectionstyle=f"arc3,rad={rad}", linestyle=ls)
    ax.add_patch(a)


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), dpi=170, bbox_inches="tight",
                facecolor="white")
    plt.close(fig)
    print("wrote", name)


# ---------------------------------------------------------------- Fig 1: System Architecture
def fig_architecture():
    fig, ax = plt.subplots(figsize=(9.2, 6.4))
    ax.set_xlim(0, 100); ax.set_ylim(0, 70); ax.axis("off")
    ax.text(50, 67, "OpsAgent — System Architecture", ha="center",
            fontsize=14, weight="bold", color=C["slate"])

    box(ax, 3, 55, 20, 8, "CloudWatch\nAlarm", C["amber"])
    box(ax, 3, 42, 20, 8, "Amazon SNS\nTopic", C["amber"])
    il = box(ax, 33, 42, 26, 10, "Incident Lambda\n(lambda_handler +\nagent)", C["blue"])
    box(ax, 3, 28, 20, 8, "Amazon ECS\nCluster", C["teal"])

    # right-side integrations
    box(ax, 70, 58, 26, 8, "CloudWatch\nLogs & Metrics", C["cyan"])
    box(ax, 70, 46, 26, 8, "Cohere Chat API\n(LLM diagnosis)", C["violet"])
    box(ax, 70, 34, 26, 8, "Amazon ECS\n(restart / scale)", C["teal"])
    box(ax, 70, 22, 26, 8, "Slack Webhook\n(notify)", C["green"])
    ddb = box(ax, 40, 24, 24, 9, "DynamoDB\nincident records", C["red"])

    # dashboard tier
    box(ax, 3, 8, 22, 9, "React / Vite\nDashboard", C["slate"])
    api = box(ax, 40, 8, 24, 9, "Dashboard API\nLambda (api.py)", C["blue"])

    arrow(ax, (13, 55), (13, 50))                  # CW -> SNS
    arrow(ax, (23, 46), (33, 47))                  # SNS -> IL
    arrow(ax, (46, 52), (70, 62), rad=-0.15)       # IL -> logs
    arrow(ax, (59, 48), (70, 50))                  # IL -> cohere
    arrow(ax, (59, 45), (70, 38), rad=0.12)        # IL -> ecs act
    arrow(ax, (52, 42), (60, 30), rad=0.1)         # IL -> slack? via
    arrow(ax, (46, 42), (52, 33), rad=0.0, color=C["red"])  # IL -> ddb
    arrow(ax, (55, 24), (78, 24), color=C["green"])         # ddb-ish? slack
    arrow(ax, (13, 28), (13, 36), style="<|-|>", color=C["teal"])  # ecs<->IL context
    arrow(ax, (25, 12), (40, 12))                  # dashboard -> api
    arrow(ax, (52, 17), (52, 24), style="<|-", color=C["red"])     # api <- ddb

    ax.text(50, 3, "Incident-processing path (left→right) is decoupled from the read-only dashboard path (bottom).",
            ha="center", fontsize=8.5, style="italic", color=C["slate"])
    save(fig, "fig_architecture.png")


# ---------------------------------------------------------------- Fig 2: Incident flow (sequence-style)
def fig_flow():
    fig, ax = plt.subplots(figsize=(8.8, 7.4))
    ax.set_xlim(0, 100); ax.set_ylim(0, 100); ax.axis("off")
    ax.text(50, 97, "OpsAgent — Incident Handling Flow", ha="center",
            fontsize=14, weight="bold", color=C["slate"])

    steps = [
        ("CloudWatch alarm fires → SNS notification", C["amber"]),
        ("Lambda normalises payload into an alert\n(service, metric, value, threshold)", C["blue"]),
        ("Gather context: last 5 min logs +\nContainer Insights metrics", C["cyan"]),
        ("Build SRE prompt → call Cohere Chat API", C["violet"]),
        ("Parse JSON diagnosis\n(root_cause, action, confidence, reasoning)", C["blue"]),
    ]
    y = 90.0
    prev_bottom = None
    for txt, col in steps:
        box(ax, 22, y, 56, 6.5, txt, col, fs=9.0)
        if prev_bottom is not None:
            arrow(ax, (50, prev_bottom), (50, y + 6.5))
        prev_bottom = y
        y -= 9.2

    # decision diamond
    dcy = y + 2.0
    ax.add_patch(plt.Polygon([(50, dcy + 6), (76, dcy), (50, dcy - 6), (24, dcy)],
                             closed=True, facecolor=C["slate"], zorder=3))
    ax.text(50, dcy, "action ∈ {restart, scale}\nAND confidence > 0.75 ?",
            ha="center", va="center", color="white", fontsize=8.6, weight="bold", zorder=4)
    arrow(ax, (50, prev_bottom), (50, dcy + 6))

    by = dcy - 20
    box(ax, 3, by, 40, 8, "YES → scoped ECS action\n(restart 1 task / scale +2, max 6)", C["green"], fs=8.6)
    box(ax, 57, by, 40, 8, "NO → escalate to on-call\n(action = none)", C["red"], fs=8.6)
    arrow(ax, (34, dcy - 3), (23, by + 8), color=C["green"])
    arrow(ax, (66, dcy - 3), (77, by + 8), color=C["red"])
    ax.text(29, dcy - 9, "yes", color=C["green"], fontsize=9, weight="bold")
    ax.text(69, dcy - 9, "no", color=C["red"], fontsize=9, weight="bold")

    fy = by - 13
    box(ax, 22, fy, 56, 8, "Notify Slack + persist incident to DynamoDB\n→ visible on React dashboard", C["blue"], fs=9.0)
    arrow(ax, (23, by), (42, fy + 8), color=C["green"])
    arrow(ax, (77, by), (58, fy + 8), color=C["red"])
    save(fig, "fig_flow.png")


# ---------------------------------------------------------------- Fig 3: Data Flow Diagram (level 1)
def fig_dfd():
    fig, ax = plt.subplots(figsize=(9.2, 6.2))
    ax.set_xlim(0, 100); ax.set_ylim(0, 70); ax.axis("off")
    ax.text(50, 67, "Level-1 Data Flow Diagram (DFD)", ha="center",
            fontsize=14, weight="bold", color=C["slate"])

    # external entities (square), processes (round), stores (open rect)
    box(ax, 2, 52, 18, 8, "CloudWatch\n(entity)", C["amber"], style="square,pad=0.02")
    box(ax, 2, 30, 18, 8, "Cohere LLM\n(entity)", C["violet"], style="square,pad=0.02")
    box(ax, 2, 8, 18, 8, "SRE / Slack\n(entity)", C["green"], style="square,pad=0.02")

    box(ax, 34, 50, 30, 10, "P1  Normalise &\nEnrich Alert", C["blue"])
    box(ax, 34, 30, 30, 10, "P2  Diagnose &\nDecide Action", C["cyan"])
    box(ax, 34, 10, 30, 10, "P3  Remediate,\nNotify & Persist", C["teal"])

    # data store
    ax.add_patch(Rectangle((74, 30), 24, 8, facecolor=C["red"], zorder=3))
    ax.text(86, 34, "D1  DynamoDB\nincidents", ha="center", va="center",
            color="white", weight="bold", fontsize=9, zorder=4)
    box(ax, 74, 10, 24, 8, "Dashboard\nAPI + UI", C["slate"])

    arrow(ax, (20, 56), (34, 55))                      # CW -> P1
    arrow(ax, (49, 50), (49, 40))                      # P1 -> P2 (alert+ctx)
    arrow(ax, (20, 34), (34, 35))                      # Cohere -> P2
    arrow(ax, (49, 30), (49, 20))                      # P2 -> P3 (decision)
    arrow(ax, (20, 12), (34, 13))                      # SRE input? escalate
    arrow(ax, (64, 15), (74, 14))                      # P3 -> UI
    arrow(ax, (60, 20), (80, 30), color=C["red"])      # P3 -> D1
    arrow(ax, (80, 30), (84, 18), style="<|-", color=C["slate"])  # D1 -> UI
    arrow(ax, (34, 12), (20, 11), color=C["green"])    # P3 -> Slack/SRE

    ax.text(50, 3, "Entities = squares · Processes = rounded · D1 = data store",
            ha="center", fontsize=8.5, style="italic", color=C["slate"])
    save(fig, "fig_dfd.png")


# ---------------------------------------------------------------- Fig 4: Component / module UML
def fig_components():
    fig, ax = plt.subplots(figsize=(9.2, 6.4))
    ax.set_xlim(0, 100); ax.set_ylim(0, 72); ax.axis("off")
    ax.text(50, 69, "Backend Component & Module View", ha="center",
            fontsize=14, weight="bold", color=C["slate"])

    mods = [
        (6, 55, "lambda_handler.py", "SNS entry · payload\nnormalisation", C["blue"]),
        (37, 55, "agent.py", "orchestration ·\nLLM call · policy", C["cyan"]),
        (68, 55, "prompts.py", "SRE prompt\nbuilder", C["violet"]),
        (6, 36, "tools.py", "CloudWatch logs/\nmetrics · ECS ops", C["teal"]),
        (37, 36, "slack.py", "incident + failure\nnotifications", C["green"]),
        (68, 36, "dynamo.py", "incident\npersistence", C["red"]),
        (21, 15, "api.py", "read-only dashboard\nAPI (/incidents /stats)", C["blue"]),
        (55, 15, "frontend/ (React)", "IncidentFeed · Detail ·\nMetricsChart · Stats", C["slate"]),
    ]
    coords = {}
    for x, y, title, sub, col in mods:
        w, h = 26, 12
        box(ax, x, y, w, h * 0.42, title, col, fs=10)
        ax.add_patch(Rectangle((x, y - h * 0.58 + 0.2), w, h * 0.58,
                     facecolor="white", edgecolor=col, linewidth=1.4, zorder=3))
        ax.text(x + w / 2, y - h * 0.58 / 2 + 0.2, sub, ha="center", va="center",
                fontsize=8, color=C["slate"], zorder=4)
        coords[title] = (x + w / 2, y, w, h)

    def link(a, b, **k):
        ca, cb = coords[a], coords[b]
        arrow(ax, (ca[0], ca[1] - 5), (cb[0], cb[1] + 5.5), **k)

    arrow(ax, (19, 55), (37, 60), style="-|>")                 # handler -> agent
    arrow(ax, (63, 60), (68, 60), style="-|>")                 # agent -> prompts
    arrow(ax, (45, 55), (30, 48), style="-|>", color=C["teal"])   # agent -> tools
    arrow(ax, (48, 55), (45, 48), style="-|>", color=C["green"])  # agent -> slack
    arrow(ax, (25, 55), (72, 42), rad=-0.2, style="-|>", color=C["red"])  # handler->dynamo
    arrow(ax, (34, 15), (81, 42), rad=-0.25, style="<|-", color=C["red"]) # api <- dynamo
    arrow(ax, (55, 21), (34, 21), style="<|-", color=C["slate"])          # ui <- api
    ax.text(50, 4, "Solid arrows = calls / dependencies between Python modules and the React client.",
            ha="center", fontsize=8.5, style="italic", color=C["slate"])
    save(fig, "fig_components.png")


# ---------------------------------------------------------------- Fig 5: Deployment diagram
def fig_deployment():
    fig, ax = plt.subplots(figsize=(9.2, 6.0))
    ax.set_xlim(0, 100); ax.set_ylim(0, 66); ax.axis("off")
    ax.text(50, 63, "AWS Deployment Topology", ha="center",
            fontsize=14, weight="bold", color=C["slate"])

    # AWS cloud boundary
    ax.add_patch(FancyBboxPatch((4, 6), 92, 50, boxstyle="round,pad=0.4",
                 linewidth=1.6, edgecolor=C["amber"], facecolor="#fff7ed", zorder=1))
    ax.text(9, 52, "AWS Cloud", fontsize=11, weight="bold", color=C["amber"])

    box(ax, 8, 40, 20, 8, "SNS Topic", C["amber"])
    box(ax, 33, 40, 26, 9, "Incident Lambda\n(container image)", C["blue"])
    box(ax, 66, 44, 26, 8, "ECR\n(image registry)", C["slate"])
    box(ax, 66, 32, 26, 8, "CloudWatch\nLogs & Metrics", C["cyan"])
    box(ax, 8, 24, 20, 8, "ECS Cluster\n+ Services", C["teal"])
    box(ax, 33, 22, 26, 8, "DynamoDB Table\n(id PK, service GSI)", C["red"])
    box(ax, 66, 20, 26, 8, "HTTP Gateway", C["violet"])
    box(ax, 33, 9, 26, 8, "Dashboard API\nLambda", C["blue"])

    # outside cloud
    box(ax, 8, 9, 20, 8, "Browser\n(React SPA)", C["green"])
    box(ax, 66, 8, 26, 7, "Slack /\nCohere API", C["green"])

    arrow(ax, (28, 44), (33, 44))
    arrow(ax, (59, 45), (66, 47), color=C["slate"])
    arrow(ax, (46, 40), (46, 30), color=C["red"])
    arrow(ax, (33, 44), (28, 28), color=C["teal"], rad=0.2)
    arrow(ax, (59, 44), (66, 36), color=C["cyan"])
    arrow(ax, (28, 13), (33, 13))
    arrow(ax, (46, 22), (46, 17), style="<|-", color=C["red"])
    arrow(ax, (59, 13), (66, 22), color=C["violet"])
    arrow(ax, (59, 44), (79, 15), rad=-0.3, color=C["green"], ls="--")
    save(fig, "fig_deployment.png")


# ---------------------------------------------------------------- Fig 6: Use-case diagram
def fig_usecase():
    fig, ax = plt.subplots(figsize=(9.0, 6.0))
    ax.set_xlim(0, 100); ax.set_ylim(0, 66); ax.axis("off")
    ax.text(50, 63, "Use-Case Diagram", ha="center",
            fontsize=14, weight="bold", color=C["slate"])

    # actors
    def actor(x, y, label):
        ax.add_patch(Circle((x, y + 4), 1.6, facecolor=C["slate"], zorder=4))
        ax.add_line(Line2D([x, x], [y + 2.4, y - 2], color=C["slate"], lw=1.6))
        ax.add_line(Line2D([x - 2.4, x + 2.4], [y + 1, y + 1], color=C["slate"], lw=1.6))
        ax.add_line(Line2D([x, x - 2], [y - 2, y - 5], color=C["slate"], lw=1.6))
        ax.add_line(Line2D([x, x + 2], [y - 2, y - 5], color=C["slate"], lw=1.6))
        ax.text(x, y - 8, label, ha="center", fontsize=9.5, weight="bold", color=C["slate"])

    actor(10, 40, "CloudWatch\n(trigger)")
    actor(10, 16, "SRE /\nOn-call")

    ucs = [
        (50, 52, "Detect & normalise alert"),
        (50, 43, "Diagnose with LLM"),
        (50, 34, "Auto-remediate (restart/scale)"),
        (50, 25, "Escalate low-confidence case"),
        (50, 16, "Notify via Slack"),
        (50, 7,  "Review incidents on dashboard"),
    ]
    for x, y, t in ucs:
        e = matplotlib.patches.Ellipse((x, y), 44, 7.2, facecolor=C["bg"],
                                       edgecolor=C["blue"], linewidth=1.4, zorder=3)
        ax.add_patch(e)
        ax.text(x, y, t, ha="center", va="center", fontsize=9.2,
                color=C["slate"], zorder=4)
        arrow(ax, (14, 40), (x - 22, y), color=C["line"], style="-", lw=1.0)

    for x, y, t in ucs[3:]:
        arrow(ax, (14, 16), (x - 22, y), color=C["line"], style="-", lw=1.0)
    save(fig, "fig_usecase.png")


if __name__ == "__main__":
    fig_architecture()
    fig_flow()
    fig_dfd()
    fig_components()
    fig_deployment()
    fig_usecase()
    print("all figures done")
