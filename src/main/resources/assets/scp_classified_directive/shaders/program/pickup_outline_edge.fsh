#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

float occupancy(vec4 sampleColor) {
    float rgb = max(sampleColor.r, max(sampleColor.g, sampleColor.b));
    return max(sampleColor.a, rgb);
}

void main() {
    float center = occupancy(texture(DiffuseSampler, texCoord));
    float neighbor = 0.0;

    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord + vec2(oneTexel.x, 0.0))));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord - vec2(oneTexel.x, 0.0))));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord + vec2(0.0, oneTexel.y))));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord - vec2(0.0, oneTexel.y))));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord + oneTexel)));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord - oneTexel)));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord + vec2(oneTexel.x, -oneTexel.y))));
    neighbor = max(neighbor, occupancy(texture(DiffuseSampler,
            texCoord + vec2(-oneTexel.x, oneTexel.y))));

    float outside = 1.0 - smoothstep(0.06, 0.30, center);
    float nearby = smoothstep(0.08, 0.42, neighbor);
    float edge = outside * nearby;

    fragColor = vec4(1.0, 1.0, 1.0, edge * 0.92);
}
