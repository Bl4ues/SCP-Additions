#version 150

uniform sampler2D Sampler0;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float r2 = dot(p, p);

    // Mild barrel distortion. The effect is strongest at the glass edges and
    // intentionally restrained in the centre so aiming remains responsive.
    vec2 warped = p * (1.0 + 0.070 * r2 + 0.018 * r2 * r2);
    vec2 uv = warped * 0.5 + 0.5;

    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    float aberration = 0.00115 * (0.35 + r2);
    float red = texture(Sampler0, uv + vec2(aberration, 0.0)).r;
    float green = texture(Sampler0, uv).g;
    float blue = texture(Sampler0, uv - vec2(aberration, 0.0)).b;
    vec3 colour = vec3(red, green, blue);

    float vignette = 1.0 - smoothstep(0.48, 1.48, r2) * 0.30;
    colour *= vignette;
    fragColor = vec4(colour, 1.0);
}
