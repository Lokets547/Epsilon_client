#version 150

uniform sampler2D InputSampler;

uniform vec2 InputResolution;
uniform float Quality;

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float thickness;
uniform float softness;

uniform float Distortion;
uniform float BlurRadius;
uniform float EdgeFeather;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 outlineColor;

float roundedRectSDF(vec2 p, vec2 halfSize, vec4 radius) {
    radius.xy = (p.x > 0.0) ? radius.xy : radius.zw;
    radius.x  = (p.y > 0.0) ? radius.x : radius.y;

    vec2 q = abs(p) - halfSize + radius.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius.x;
}

vec4 sampleScene(vec2 uv) {
    return texture(InputSampler, uv);
}

vec4 blur5(vec2 uv, vec2 texel) {
    vec4 c = sampleScene(uv) * 0.4;
    c += sampleScene(uv + texel * vec2( BlurRadius,  0.0)) * 0.15;
    c += sampleScene(uv + texel * vec2(-BlurRadius,  0.0)) * 0.15;
    c += sampleScene(uv + texel * vec2( 0.0,  BlurRadius)) * 0.15;
    c += sampleScene(uv + texel * vec2( 0.0, -BlurRadius)) * 0.15;
    return c;
}

out vec4 fragColor;

void main() {
    vec2 fragPix = gl_FragCoord.xy;

    vec2 rectCenter = location + size * 0.5;
    vec2 halfSize = size * 0.5;
    vec2 rel = (fragPix - rectCenter) / halfSize;

    float distEdge = roundedRectSDF(rel * halfSize, halfSize, radius);

    float feather = max(softness + EdgeFeather, 0.0001);
    float outerAlpha = 1.0 - smoothstep(0.0, feather, distEdge);
    if (outerAlpha <= 0.0) { discard; }

    float borderMask = outerAlpha;
    if (thickness > 0.0) {
        float innerFeather = max(softness, 0.0001);
        float innerBand = smoothstep(-thickness - innerFeather * 1.0, -thickness, distEdge);
        float outerBand = 1.0 - smoothstep(0.0, innerFeather, distEdge);
        borderMask = clamp(innerBand * outerBand, 0.0, 1.0);
    }

    float lenRel = length(rel);
    float k = Distortion;
    float lenDist = lenRel + k * pow(lenRel, 3.0);
    vec2 relDist = rel * (lenDist / max(lenRel, 1e-5));

    relDist = clamp(relDist, vec2(-1.2), vec2(1.2));

    vec2 samplePix = rectCenter + relDist * halfSize;
    vec2 uv = samplePix / InputResolution;

    vec2 texel = 1.0 / InputResolution;
    vec4 col = blur5(uv, texel);

    fragColor = vec4(col.rgb, borderMask * col.a);
}
