#version 150
uniform sampler2D Sampler0; // Encoded surface normals and coverage, with texture cutouts.
uniform sampler2D Sampler1; // Nearest surface of that entity, independent of world depth.
uniform mat4 InverseViewProjection;
uniform vec4 OriginHeight;
uniform vec3 Tint;
uniform vec4 Scan; // height progress, continuous signed velocity, trail width / body height, brightness
uniform float Opacity;
uniform vec2 PixelSize;
in vec2 texCoord;
out vec4 fragColor;

float hash12(vec2 value) {
    vec3 p = fract(vec3(value.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec2 surfaceProjection(vec3 position, vec3 normal) {
    normal = abs(normal);
    if (normal.y >= normal.x && normal.y >= normal.z) return position.xz;
    return normal.x >= normal.z ? position.zy : position.xy;
}

vec3 reconstruct(vec2 uv, float depth) {
    vec4 relative = InverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return relative.xyz / relative.w - OriginHeight.xyz;
}

vec4 shade(vec2 uv, vec4 surfaceData, float depth) {
    vec3 body = reconstruct(uv, depth);
    vec3 normal = surfaceData.rgb * 2.0 - 1.0;
    // Analytic pixel footprints avoid undefined derivatives in partially covered sample quads.
    vec3 px = reconstruct(uv + vec2(PixelSize.x, 0.0), depth) - body;
    vec3 py = reconstruct(uv + vec2(0.0, PixelSize.y), depth) - body;
    float height = OriginHeight.w;
    float scanY = height * mix(0.035, 0.965, Scan.x);
    float band = max(height * Scan.z, 0.035);
    float delta = scanY - body.y;
    float footprint = abs(px.y) + abs(py.y);
    float frontWidth = max(height * 0.018, footprint * 0.75);
    float front = exp(-pow(delta / frontWidth, 2.0));
    float glow = exp(-pow(delta / max(band * 0.32, frontWidth * 2.5), 2.0));
    float below = smoothstep(-frontWidth, frontWidth, delta) * (1.0 - smoothstep(0.0, band, delta));
    float above = smoothstep(-frontWidth, frontWidth, -delta) * (1.0 - smoothstep(0.0, band, -delta));
    float travel = smoothstep(-0.55, 0.55, Scan.y);
    float trail = mix(above, below, travel);

    // The same square-cell vocabulary as Frag, anchored to the target instead of the screen.
    float cellSize = max(height * 0.026, 0.025);
    vec2 surface = surfaceProjection(body, normal) / cellSize;
    vec2 cellFootprint = (abs(surfaceProjection(px, normal)) + abs(surfaceProjection(py, normal))) / cellSize;
    float footprintCell = max(cellFootprint.x, cellFootprint.y);
    vec2 cell = floor(surface);
    vec2 squareUv = abs(fract(surface) - 0.5);
    float aa = max(0.02, footprintCell * 0.5);
    float detail = 1.0 - smoothstep(0.3, 0.8, footprintCell);
    float squareCore = mix(0.8, 1.0 - smoothstep(0.40 - aa, 0.46 + aa, max(squareUv.x, squareUv.y)), detail);
    float noise = mix(0.5, hash12(cell), detail);
    float infection = trail * mix(0.40, 0.8, noise) * mix(0.65, 1.0, squareCore);
    float sparks = smoothstep(0.82, 0.94, noise) * squareCore * trail * trail * detail;
    float lineDetail = 1.0 - smoothstep(1.0, 3.14, footprint / height * 360.0);
    float scanlines = 0.5 + 0.5 * sin(body.y / height * 360.0) * lineDetail;
    vec3 color = mix(Tint, vec3(1.0), clamp(front * 0.85 + sparks * 0.15, 0.0, 1.0));
    float intensity = (front * 0.9 + glow * 0.20 + infection * 0.70 + sparks * 0.18 + scanlines * trail * 0.04) * Scan.w;
    float alpha = clamp(intensity * Opacity, 0.0, 0.95);
    return vec4(color * alpha, alpha);
}

void main() {
    vec4 accumulated = vec4(0.0);
    float nearestDepth = 1.0;
    // Four subpixel samples resolve the supersampled silhouette and procedural pattern together.
    for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
        vec2 uv = texCoord + (vec2(float(x), float(y)) - 0.5) * PixelSize * 0.5;
        vec4 surface = texture(Sampler0, uv);
        float depth = texture(Sampler1, uv).r;
        if (surface.a > 0.1 && depth < 1.0) {
            vec4 value = shade(uv, surface, depth);
            accumulated += value;
            if (value.a > 0.001) nearestDepth = min(nearestDepth, depth);
        }
    }
    if (accumulated.a < 0.004) discard;
    fragColor = vec4(accumulated.rgb / accumulated.a, accumulated.a * 0.25);
    // Test against world depth without reading an attachment of the active framebuffer.
    gl_FragDepth = max(0.0, nearestDepth - 0.000002);
}
