#version 150

uniform sampler2D Sampler0;
uniform float Strength;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 34.5);
    return fract(p.x * p.y);
}

void main() {
    vec3 source = texture(Sampler0, texCoord).rgb;
    float amount = smoothstep(0.0, 1.0, clamp(Strength, 0.0, 1.0));
    if (amount <= 0.0001) {
        fragColor = vec4(source, 1.0);
        return;
    }

    vec2 dimensions = vec2(textureSize(Sampler0, 0));
    vec2 texel = 1.0 / max(dimensions, vec2(1.0));

    float luma = dot(source, vec3(0.2126, 0.7152, 0.0722));
    float leftLuma = dot(texture(Sampler0,
            texCoord - vec2(texel.x * 1.5, 0.0)).rgb,
            vec3(0.2126, 0.7152, 0.0722));
    float rightLuma = dot(texture(Sampler0,
            texCoord + vec2(texel.x * 1.5, 0.0)).rgb,
            vec3(0.2126, 0.7152, 0.0722));
    float upLuma = dot(texture(Sampler0,
            texCoord + vec2(0.0, texel.y * 1.5)).rgb,
            vec3(0.2126, 0.7152, 0.0722));
    float downLuma = dot(texture(Sampler0,
            texCoord - vec2(0.0, texel.y * 1.5)).rgb,
            vec3(0.2126, 0.7152, 0.0722));
    float neighbourhood = (leftLuma + rightLuma + upLuma + downLuma) * 0.25;

    // Sensor gain lifts information that is already present in very dark pixels
    // without adding a coloured tint. The result intentionally remains true
    // monochrome so night vision and the black-and-white filter are one effect.
    float amplified = pow(clamp(max(luma, neighbourhood * 0.72), 0.0, 1.0),
            0.46) * 1.18;
    float localDetail = (luma - neighbourhood) * 0.62;
    amplified = clamp(amplified + localDetail, 0.0, 1.0);

    float grain = hash21(texCoord * dimensions
            + vec2(floor(Time * 19.0), floor(Time * 13.0))) - 0.5;
    amplified = clamp(amplified + grain * 0.035, 0.0, 1.0);

    vec2 p = texCoord * 2.0 - 1.0;
    float r2 = dot(p, p);
    float sensorVignette = 1.0 - smoothstep(0.52, 1.72, r2) * 0.18;
    amplified *= sensorVignette;

    vec3 monochrome = vec3(amplified);
    fragColor = vec4(mix(source, monochrome, amount), 1.0);
}
