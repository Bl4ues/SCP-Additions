#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D EdgeSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    vec4 outline = texture(EdgeSampler, texCoord);
    float edge = clamp(outline.a, 0.0, 1.0);

    vec3 composed = mix(scene.rgb, vec3(1.0), edge);
    fragColor = vec4(composed, scene.a);
}
