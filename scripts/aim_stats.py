import csv
import json
from pathlib import Path

COLUMNS = [
    "yaw_delta",
    "pitch_delta",
    "target_yaw",
    "target_pitch",
    "distance",
    "since_attack",
    "yaw",
    "pitch",
]

PROFILE_POINTS = 16
DATASET_PATH = Path(
    r"c:\Users\yurwx\Desktop\Dettex\src\main\resources\assets\minecraft\ai\aim_dataset.csv"
)
PROFILE_PATH = DATASET_PATH.with_name("aim_profile.json")


def percentile(values, pct):
    if not values:
        return float("nan")
    sorted_vals = sorted(values)
    k = (len(sorted_vals) - 1) * pct
    f = int(k)
    c = min(f + 1, len(sorted_vals) - 1)
    if f == c:
        return sorted_vals[f]
    d0 = sorted_vals[f] * (c - k)
    d1 = sorted_vals[c] * (k - f)
    return d0 + d1


def ensure_variation(sequence, epsilon=1e-3):
    adjusted = []
    last = None
    for idx, value in enumerate(sequence):
        current = float(value)
        if last is not None and abs(current - last) < epsilon:
            current += epsilon * (1 if idx % 2 == 0 else -1)
        adjusted.append(current)
        last = current
    return adjusted


def smooth_samples(values, points, window_radius):
    if not values:
        return [0.0 for _ in range(points)]
    max_index = len(values) - 1
    result = []
    for i in range(points):
        t = i / (points - 1) if points > 1 else 0.0
        center = t * max_index
        start = max(0, int(center) - window_radius)
        end = min(len(values), int(center) + window_radius + 1)
        segment = values[start:end]
        if not segment:
            result.append(0.0)
        else:
            result.append(sum(segment) / len(segment))
    return result


def round_list(values, precision=6):
    return [round(v, precision) for v in values]


def build_profile(records):
    if not records:
        return {}

    abs_yaw = [abs(rec["yaw_delta"]) for rec in records]
    abs_pitch = [abs(rec["pitch_delta"]) for rec in records]

    max_abs_yaw = max(abs_yaw) if abs_yaw else 0.0
    max_abs_pitch = max(abs_pitch) if abs_pitch else 0.0

    window_radius = max(8, len(records) // (PROFILE_POINTS * 2))
    yaw_samples = smooth_samples(abs_yaw, PROFILE_POINTS, window_radius)
    pitch_samples = smooth_samples(abs_pitch, PROFILE_POINTS, window_radius)

    yaw_limit_curve = ensure_variation(
        [min(max_abs_yaw * 1.26, sample * 1.58 + 5.2) for sample in yaw_samples]
    )
    pitch_limit_curve = ensure_variation(
        [min(max_abs_pitch * 1.34, sample * 1.52 + 3.6) for sample in pitch_samples]
    )

    yaw_speed_curve = ensure_variation(
        [min(limit, sample * 0.94 + 1.1) for sample, limit in zip(yaw_samples, yaw_limit_curve)]
    )
    pitch_speed_curve = ensure_variation(
        [min(limit, sample * 0.98 + 0.8) for sample, limit in zip(pitch_samples, pitch_limit_curve)]
    )

    yaw_inertia_curve = ensure_variation(
        [min(0.72, max(0.22, (sample / max_abs_yaw if max_abs_yaw else 0.0) * 0.42 + 0.26)) for sample in yaw_samples]
    )
    pitch_inertia_curve = ensure_variation(
        [min(0.64, max(0.2, (sample / max_abs_pitch if max_abs_pitch else 0.0) * 0.36 + 0.24)) for sample in pitch_samples]
    )

    yaw_blend_curve = ensure_variation(
        [min(0.82, max(0.24, 0.24 + (sample / max_abs_yaw if max_abs_yaw else 0.0) * 0.48)) for sample in yaw_samples]
    )
    pitch_blend_curve = ensure_variation(
        [min(0.68, max(0.22, 0.26 + (sample / max_abs_pitch if max_abs_pitch else 0.0) * 0.42)) for sample in pitch_samples]
    )

    yaw_min_step_curve = ensure_variation(
        [min(4.2, max(0.62, 0.58 + sample * 0.035)) for sample in yaw_samples]
    )
    pitch_min_step_curve = ensure_variation(
        [min(2.8, max(0.4, 0.42 + sample * 0.028)) for sample in pitch_samples]
    )

    domain = [i / (PROFILE_POINTS - 1) if PROFILE_POINTS > 1 else 0.0 for i in range(PROFILE_POINTS)]

    profile = {
        "samples": len(records),
        "domain": round_list(domain, 6),
        "max_abs_yaw": round(max_abs_yaw, 6),
        "max_abs_pitch": round(max_abs_pitch, 6),
        "yaw_limit": round_list(yaw_limit_curve),
        "pitch_limit": round_list(pitch_limit_curve),
        "yaw_speed": round_list(yaw_speed_curve),
        "pitch_speed": round_list(pitch_speed_curve),
        "yaw_inertia": round_list(yaw_inertia_curve, 6),
        "pitch_inertia": round_list(pitch_inertia_curve, 6),
        "yaw_blend": round_list(yaw_blend_curve, 6),
        "pitch_blend": round_list(pitch_blend_curve, 6),
        "yaw_min_step": round_list(yaw_min_step_curve, 6),
        "pitch_min_step": round_list(pitch_min_step_curve, 6),
    }

    return profile


def main():
    stats = {col: [] for col in COLUMNS}
    records = []

    with DATASET_PATH.open(newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            record = {}
            skip = False
            for col in COLUMNS:
                try:
                    value = float(row[col])
                except (KeyError, ValueError):
                    skip = True
                    break
                stats[col].append(value)
                record[col] = value
            if not skip:
                records.append(record)

    records.sort(key=lambda item: item.get("distance", 0.0))

    for col, values in stats.items():
        if not values:
            continue
        avg = sum(values) / len(values)
        min_v = min(values)
        max_v = max(values)
        abs_avg = sum(abs(v) for v in values) / len(values)
        p50 = percentile(values, 0.5)
        p75 = percentile(values, 0.75)
        p90 = percentile(values, 0.9)
        abs_values = [abs(v) for v in values]
        abs_p50 = percentile(abs_values, 0.5)
        abs_p75 = percentile(abs_values, 0.75)
        abs_p90 = percentile(abs_values, 0.9)
        print(
            f"{col}: avg={avg:.4f} abs_avg={abs_avg:.4f} min={min_v:.4f} max={max_v:.4f} "
            f"p50={p50:.4f} p75={p75:.4f} p90={p90:.4f} abs_p50={abs_p50:.4f} abs_p75={abs_p75:.4f} abs_p90={abs_p90:.4f}"
        )

    profile = build_profile(records)
    if profile:
        PROFILE_PATH.write_text(json.dumps(profile, indent=2))
        print(f"Saved profile with {PROFILE_POINTS} points to {PROFILE_PATH}")
    else:
        print("No valid records found; profile was not generated")


if __name__ == "__main__":
    main()
