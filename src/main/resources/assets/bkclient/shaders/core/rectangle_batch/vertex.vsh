#version 150

#moj_import <bkclient:common.glsl>

// Batched twin of core/position_color for core/rectangle: the rectangle size travels in UV0
// so many differently sized rounded rects can share one draw call.
in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
flat out vec2 RectSize;
out vec4 FragColor;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    RectSize = UV0;
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
