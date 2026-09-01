#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    float center = texture(DiffuseSampler, texCoord).a;
    float neighbor = 0.0;

    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord + vec2(oneTexel.x, 0.0)).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord - vec2(oneTexel.x, 0.0)).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord + vec2(0.0, oneTexel.y)).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord - vec2(0.0, oneTexel.y)).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord + oneTexel).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord - oneTexel).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord + vec2(oneTexel.x, -oneTexel.y)).a);
    neighbor = max(neighbor, texture(DiffuseSampler,
            texCoord + vec2(-oneTexel.x, oneTexel.y)).a);

    float outside = 1.0 - smoothstep(0.06, 0.30, center);
    float nearby = smoothstep(0.08, 0.42, neighbor);
    float edge = outside * nearby;

    fragColor = vec4(1.0, 1.0, 1.0, edge * 0.92);
}
