#version 150
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec3 surfaceNormal;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    // Store actual face normals: depth derivatives become unstable along silhouette edges.
    vec3 normal = surfaceNormal / max(length(surfaceNormal), 0.0001);
    fragColor = vec4(normal * 0.5 + 0.5, 1.0);
}
