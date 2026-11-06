#version 150
uniform float uTime, uSpeed, uAlpha, uFov, uBrightness;
uniform vec2 uResolution;
uniform vec3 uCamRight, uCamUp, uCamForward;
uniform vec3 uStarColor, uHorizonColor;
uniform float uAurora, uStarAmount, uGlow, uFlow, uStars;
in vec2 vScreen;
out vec4 fragColor;

vec3 skyRay() {
    float f = tan(radians(uFov) * .5);
    return normalize(uCamForward + uCamRight * vScreen.x * (uResolution.x / max(1., uResolution.y)) * f + uCamUp * vScreen.y * f);
}
float hash(vec3 p) {
    p = fract(p * .1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

// Meteors use small depth-tested world quads in world_streak; this pass only draws the sky.
void main() {
    vec3 ray = skyRay();
    float angle = atan(ray.z, ray.x);
    float time = uTime * uSpeed * uFlow;
    float horizon = exp(-abs(ray.y) * 5.5);
    float belt = exp(-abs(ray.y) * 130.);
    vec3 col = vec3(.012, .004, .034) + uHorizonColor * horizon * .31 * uGlow;
    col += mix(uHorizonColor, vec3(1.), .28) * belt * uGlow * .58;

    // Integer angular frequencies close the dome seamlessly. Light evolves in world
    // directions with time, independently of camera translation or head movement.
    for (int i = 0; i < 2; i++) {
        float layer = float(i);
        float a = angle + layer * 1.7;
        float edge = .10 + layer * .12 + .035 * sin(a * 3. + time * .12)
                     + .016 * sin(a * 11. - time * .09);
        float strands = .48 + .21 * sin(a * 23. + sin(a * 7. + time * .08) * 2.)
                            + .17 * sin(a * 61. - time * .13)
                            + .08 * sin(a * 113. + time * .06);
        float curtain = smoothstep(edge - .028, edge + .022, ray.y)
                      * exp(-max(ray.y - edge, 0.) * (5.5 + layer * 2.))
                      * (.12 + strands * strands * .8);
        vec3 tint = mix(uHorizonColor, uStarColor, .12 + layer * .2);
        col += tint * curtain * uAurora * (1. - layer * .38);
    }

    vec3 starCells = ray * 190.;
    vec3 id = floor(starCells);
    vec3 local = fract(starCells) - .5;
    float seed = hash(id);
    // Derivative-width cores stay smooth as resolution and FOV change.
    float starWidth = max(.05, length(fwidth(starCells)) * .3);
    float star = (1. - smoothstep(.035, .12 + starWidth, length(local)))
               * step(1. - .019 * uStarAmount, seed);
    float twinkle = .78 + .22 * sin(time * .9 + seed * 120.);
    col += uStarColor * star * twinkle * uStars * smoothstep(-.03, .15, ray.y);

    fragColor = vec4(clamp(col * uBrightness, 0., 1.), uAlpha);
}
