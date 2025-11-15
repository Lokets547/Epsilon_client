#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float thickness;
uniform float softness;

uniform sampler2D InputSampler;
uniform vec2 InputResolution;
uniform float Quality;
uniform float Distortion;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 outlineColor;

in vec2 texCoord;
out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 halfSize, vec4 radius) {
    radius.xy = (center.x > 0.0) ? radius.xy : radius.zw;
    radius.x  = (center.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(center) - halfSize + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

vec4 createGradient(vec2 coords, vec4 c1, vec4 c2, vec4 c3, vec4 c4) {
    vec4 color = mix(mix(c1, c2, coords.y), mix(c3, c4, coords.y), coords.x);
    color += mix(0.0019607843, -0.0019607843, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));
    return color;
}

vec2 computeDistortedUV(vec2 fragPix) {
    vec2 rectCenter = location + size * 0.5;
    vec2 halfSize = size * 0.5;
    vec2 rel = (fragPix - rectCenter) / halfSize;

    float lenRel = length(rel);
    float lenDist = lenRel + Distortion * pow(lenRel, 3.0);
    vec2 relDist = rel * (lenDist / max(lenRel, 1e-5));
    relDist = clamp(relDist, vec2(-1.2), vec2(1.2));

    vec2 samplePix = rectCenter + relDist * halfSize;
    vec2 uv = samplePix / InputResolution;
    return clamp(uv, vec2(0.0), vec2(1.0));
}

vec4 blurGlass() {
    #define TAU 6.28318530718
    vec4 rectColor = color1;
    vec2 radiusVec = (Quality * 3.0) / InputResolution.xy;
    vec2 uv = computeDistortedUV(gl_FragCoord.xy);
    vec4 blurColor = texture(InputSampler, uv);
    float sampleCount = 1.0;

    float step = TAU / 32.0;

    for (float d = 0.0; d < TAU; d += step) {
        for (float i = 0.1; i <= 1.6; i += 0.1) {
            blurColor += texture(InputSampler, uv + vec2(cos(d), sin(d)) * radiusVec * i);
            sampleCount += 1.0;
        }
    }
    blurColor /= sampleCount;
    return vec4((blurColor * (1.0 - rectColor.a)).rgb, rectColor.a) + rectColor;
}

void main() {
    vec2 halfSize = size * 0.5;
    vec2 localPos = gl_FragCoord.xy - location - halfSize;

    float distEdge = roundedBoxSDF(localPos, halfSize, radius);
    float smoothedAlpha = 1.0 - smoothstep(-1.0, softness + 1.0, distEdge);
    vec4 blurred = blurGlass();
    fragColor = vec4(blurred.rgb, blurred.a * smoothedAlpha);
}
