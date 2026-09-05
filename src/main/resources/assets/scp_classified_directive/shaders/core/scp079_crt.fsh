#version 150

uniform sampler2D Sampler0;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float r2 = dot(p, p);

    float warp = 1.0 + 0.055 * r2 + 0.012 * r2 * r2;
    vec2 uv = p * warp * 0.5 + 0.5;

    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec2 dimensions = vec2(textureSize(Sampler0, 0));
    vec2 texel = 1.0 / max(dimensions, vec2(1.0));

    // Very small horizontal time-dependent instability, strongest near edges.
    float lineNoise = hash21(vec2(floor(uv.y * dimensions.y * 0.16),
            floor(Time * 18.0)));
    float jitter = (lineNoise - 0.5) * texel.x * (0.30 + r2 * 1.35);
    vec2 signalUv = uv + vec2(jitter, 0.0);

    // Analogue softness keeps text recognizable without looking like a modern overlay.
    vec3 colour = texture(Sampler0, signalUv).rgb * 0.48;
    colour += texture(Sampler0, signalUv + vec2(texel.x * 1.15, 0.0)).rgb * 0.13;
    colour += texture(Sampler0, signalUv - vec2(texel.x * 1.15, 0.0)).rgb * 0.13;
    colour += texture(Sampler0, signalUv + vec2(0.0, texel.y * 1.10)).rgb * 0.13;
    colour += texture(Sampler0, signalUv - vec2(0.0, texel.y * 1.10)).rgb * 0.13;

    float aberration = 0.00125 * (0.28 + r2);
    colour.r = mix(colour.r,
            texture(Sampler0, signalUv + vec2(aberration, 0.0)).r, 0.38);
    colour.b = mix(colour.b,
            texture(Sampler0, signalUv - vec2(aberration, 0.0)).b, 0.38);

    // Scanlines are derived from warped source coordinates, so the complete UI
    // and the lines themselves bend around the same CRT glass.
    float scan = 0.875 + 0.125 * sin(
            signalUv.y * dimensions.y * 3.14159265359);
    colour *= scan;

    float grain = hash21(signalUv * dimensions
            + vec2(floor(Time * 24.0), floor(Time * 13.0))) - 0.5;
    colour += grain * 0.018;

    // A faint rolling luminance band avoids a perfectly static digital signal.
    float roll = fract(signalUv.y + Time * 0.085);
    float band = 1.0 - smoothstep(0.0, 0.055, abs(roll - 0.5));
    colour += band * 0.010;

    vec3 horizontal = texture(Sampler0,
            signalUv + vec2(texel.x * 2.4, 0.0)).rgb
            + texture(Sampler0, signalUv - vec2(texel.x * 2.4, 0.0)).rgb;
    colour += max(horizontal - vec3(1.05), vec3(0.0)) * 0.030;

    float vignette = 1.0 - smoothstep(0.42, 1.46, r2) * 0.38;
    colour *= vignette;
    fragColor = vec4(max(colour, vec3(0.0)), 1.0);
}
