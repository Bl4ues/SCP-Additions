#version 150

uniform sampler2D Sampler0;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float r2 = dot(p, p);

    // The whole rendered 079 display lives on the same curved glass. All
    // effects below sample this warped coordinate so scanlines and UI bend with
    // the image instead of continuing flat past the CRT edge.
    vec2 warped = p * (1.0 + 0.070 * r2 + 0.018 * r2 * r2);
    vec2 uv = warped * 0.5 + 0.5;

    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec2 dimensions = vec2(textureSize(Sampler0, 0));
    vec2 texel = 1.0 / max(dimensions, vec2(1.0));

    // Slight analogue softness. It is deliberately restrained: text remains
    // readable, but no element looks like a perfectly crisp modern overlay.
    vec3 colour = texture(Sampler0, uv).rgb * 0.52;
    colour += texture(Sampler0, uv + vec2(texel.x * 1.15, 0.0)).rgb * 0.12;
    colour += texture(Sampler0, uv - vec2(texel.x * 1.15, 0.0)).rgb * 0.12;
    colour += texture(Sampler0, uv + vec2(0.0, texel.y * 1.10)).rgb * 0.12;
    colour += texture(Sampler0, uv - vec2(0.0, texel.y * 1.10)).rgb * 0.12;

    float aberration = 0.00110 * (0.32 + r2);
    colour.r = mix(colour.r,
            texture(Sampler0, uv + vec2(aberration, 0.0)).r, 0.34);
    colour.b = mix(colour.b,
            texture(Sampler0, uv - vec2(aberration, 0.0)).b, 0.34);

    // Scanlines use warped source Y, so they follow the same barrel distortion
    // as the image and never escape through the black CRT corners.
    float scan = 0.925 + 0.075 * sin(
            uv.y * dimensions.y * 3.14159265359);
    colour *= scan;

    // Faint phosphor bloom from the horizontally smeared signal.
    vec3 horizontal = texture(Sampler0,
            uv + vec2(texel.x * 2.4, 0.0)).rgb
            + texture(Sampler0, uv - vec2(texel.x * 2.4, 0.0)).rgb;
    colour += max(horizontal - vec3(1.05), vec3(0.0)) * 0.025;

    float vignette = 1.0 - smoothstep(0.48, 1.48, r2) * 0.32;
    colour *= vignette;
    fragColor = vec4(colour, 1.0);
}
