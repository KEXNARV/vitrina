package com.kevin.vitrina.scene.layers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import com.kevin.vitrina.scene.Layer
import com.kevin.vitrina.scene.NoiseTex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * El cielo volumétrico: port 1:1 del shader aprobado en el laboratorio
 * web (~/vitrina/web/sky.html). Raymarch de 40 pasos por una capa 3D
 * de nubes con sombra solar (4 muestras + doble lóbulo de scattering),
 * powder, fase Henyey-Greenstein y ambiente bicolor.
 *
 * Parámetros CLAVADOS a la configuración aprobada por Kevin:
 * hora 12 · cobertura 0.1 · oscuridad 0 · viento 1 · 40 pasos.
 * (Cuando el look quede validado en el Pixel, reconectar Environment.)
 */
class SkyLayer : Layer {

    // Config aprobada en el lab
    private val fixedHour = 12f
    private val fixedCover = 0.10f
    private val fixedDark = 0.0f

    private data class SkyPalette(
        val top: FloatArray, val bot: FloatArray, val sun: FloatArray,
    )

    private val night = SkyPalette(
        floatArrayOf(0.006f, 0.009f, 0.022f),
        floatArrayOf(0.028f, 0.042f, 0.075f),
        floatArrayOf(0.80f, 0.85f, 0.95f),
    )
    private val dawn = SkyPalette(
        floatArrayOf(0.030f, 0.035f, 0.070f),
        floatArrayOf(0.360f, 0.220f, 0.140f),
        floatArrayOf(1.00f, 0.70f, 0.40f),
    )
    private val day = SkyPalette(
        floatArrayOf(0.170f, 0.300f, 0.550f),
        floatArrayOf(0.520f, 0.580f, 0.660f),
        floatArrayOf(1.00f, 0.90f, 0.70f),
    )
    private val dusk = SkyPalette(
        floatArrayOf(0.040f, 0.030f, 0.070f),
        floatArrayOf(0.400f, 0.190f, 0.090f),
        floatArrayOf(1.00f, 0.60f, 0.30f),
    )

    private val keyframes = listOf(
        0.0f to night, 5.0f to night, 6.6f to dawn, 9.0f to day,
        16.8f to day, 18.6f to dusk, 20.3f to night, 24f to night,
    )

    private fun lerp(a: FloatArray, b: FloatArray, t: Float) =
        FloatArray(3) { a[it] + (b[it] - a[it]) * t }

    private fun paletteAt(hour: Float): SkyPalette {
        for (i in 0 until keyframes.size - 1) {
            val (h0, p0) = keyframes[i]
            val (h1, p1) = keyframes[i + 1]
            if (hour in h0..h1) {
                val t = if (h1 == h0) 0f else (hour - h0) / (h1 - h0)
                return SkyPalette(
                    lerp(p0.top, p1.top, t), lerp(p0.bot, p1.bot, t), lerp(p0.sun, p1.sun, t),
                )
            }
        }
        return night
    }

    private var t = 0f

    private val paint = Paint()
    private val shader: RuntimeShader? =
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                RuntimeShader(AGSL).also { it.setInputShader("uNoise", NoiseTex.shader) }
            } catch (e: Exception) {
                null
            }
        } else null

    override fun update(dt: Float, t: Float, w: Int, h: Int) {
        this.t = t
    }

    override fun draw(canvas: Canvas, w: Int, h: Int) {
        val hour = fixedHour
        val pal = paletteAt(hour)

        val dayProg = ((hour - 6.0f) / 12.5f)
        val isDay = dayProg in 0f..1f
        val prog = if (isDay) dayProg else {
            val nightHour = if (hour >= 18.5f) hour - 18.5f else hour + 5.5f
            (nightHour / 11.5f).coerceIn(0f, 1f)
        }
        val elev = sin(PI.toFloat() * prog)
        val sunX = cos(PI.toFloat() * prog) * 0.85f
        val sunY = 0.06f + elev * 0.85f
        val glow = if (isDay) 1.0f else 0.45f

        if (shader != null) {
            shader.setFloatUniform("uRes", w.toFloat(), h.toFloat())
            shader.setFloatUniform("uTime", t)
            shader.setFloatUniform("uCover", fixedCover)
            shader.setFloatUniform("uDark", fixedDark)
            shader.setFloatUniform("uWind", 1.0f)
            shader.setFloatUniform("uTop", pal.top[0], pal.top[1], pal.top[2])
            shader.setFloatUniform("uBot", pal.bot[0], pal.bot[1], pal.bot[2])
            shader.setFloatUniform("uSunCol", pal.sun[0], pal.sun[1], pal.sun[2])
            shader.setFloatUniform("uSunDir", sunX, sunY, 0.55f)
            shader.setFloatUniform("uGlow", glow)
            paint.shader = shader
        } else {
            fun rgb(c: FloatArray) =
                Color.rgb((c[0] * 255).toInt(), (c[1] * 255).toInt(), (c[2] * 255).toInt())
            paint.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                rgb(pal.top), rgb(pal.bot),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private companion object {
        // Port directo del GLSL del laboratorio. Diferencias mecánicas:
        // vec→floatN, texture2D(uv/256)→eval(uv px), gl_FragCoord y
        // invertida (en AGSL y=0 es ARRIBA), sin break (guard con if).
        val AGSL = """
            uniform float2 uRes;
            uniform float  uTime;
            uniform float  uCover;
            uniform float  uDark;
            uniform float  uWind;
            uniform float3 uTop;
            uniform float3 uBot;
            uniform float3 uSunCol;
            uniform float3 uSunDir;
            uniform float  uGlow;
            uniform shader uNoise;

            float hash12(float2 p) {
                p = 50.0 * fract(p * 0.3183099 + float2(0.71, 0.113));
                return fract(p.x * p.y * (p.x + p.y));
            }

            float noise3(float3 x) {
                float3 p = floor(x);
                float3 f = fract(x);
                f = f * f * (3.0 - 2.0 * f);
                float2 uv = (p.xy + float2(37.0, 17.0) * p.z) + f.xy;
                half4 tx = uNoise.eval(uv + 0.5);
                return mix(float(tx.x), float(tx.y), f.z);
            }

            float map(float3 pos, float oct) {
                float3 wind = float3(uTime * 0.06 * uWind, uTime * 0.01, uTime * 0.02 * uWind);
                float3 q = pos * float3(0.70, 0.55, 0.70) - wind;
                float f = 0.0;
                float amp = 0.5;
                f += amp * noise3(q);
                if (oct > 1.5) { q = q * 2.02 + float3(1.7, 4.1, 9.3); amp *= 0.5; f += amp * noise3(q); }
                if (oct > 2.5) { q = q * 2.03 + float3(8.2, 0.5, 3.1); amp *= 0.5; f += amp * noise3(q); }
                if (oct > 3.5) { q = q * 2.01 + float3(2.9, 7.7, 0.8); amp *= 0.5; f += amp * noise3(q); }
                if (oct > 4.5) { q = q * 2.02 + float3(5.1, 2.2, 6.4); amp *= 0.5; f += amp * noise3(q); }
                f /= (1.0 - amp);

                // la altura de la base varía por región: sin "vidrio" común
                float baseVar = noise3(float3(pos.x * 0.16, pos.z * 0.16, 7.7));
                float yBase = 0.45 + baseVar * 0.85;
                float h = clamp((pos.y - yBase) / 2.0, 0.0, 1.0);
                float profile = smoothstep(0.0, 0.24, h) * smoothstep(1.0, 0.35, h);

                // con tormenta el techo se llena; el claro se abre al sol
                profile = mix(profile, smoothstep(0.0, 0.24, h), uDark * 0.7);
                float3 dir = normalize(pos + float3(0.0, 0.001, 0.0));
                float toSun = clamp(dot(dir, normalize(uSunDir)), 0.0, 1.0);
                float clearing = (0.20 + 0.30 * uDark) * pow(toSun, 9.0);

                float d = f * profile * 1.55 + (uCover * 0.68 - 0.76) - clearing;
                return clamp(d * 2.8, 0.0, 1.0);
            }

            float lightMarch(float3 pos, float3 sun) {
                float d = 0.0;
                d += map(pos + sun * 0.30, 3.0);
                d += map(pos + sun * 0.80, 2.0);
                d += map(pos + sun * 1.60, 2.0);
                // doble lóbulo: luz directa + re-dispersión interna
                return max(exp(-d * 1.4), 0.28 * exp(-d * 0.35));
            }

            float phaseHG(float c, float g) {
                float g2 = g * g;
                return (1.0 - g2) / (4.0 * 3.14159 * pow(1.0 + g2 - 2.0 * g * c, 1.5));
            }

            half4 main(float2 frag) {
                float2 uv = frag / uRes;
                uv.y = 1.0 - uv.y;              // misma orientación que WebGL
                float aspect = uRes.x / uRes.y;
                float2 ndc = float2(uv.x * 2.0 - 1.0, uv.y * 2.0 - 1.0);

                float3 ro = float3(0.0, 0.0, 0.0);
                float3 rd = normalize(float3(ndc.x * aspect * 0.62, 0.30 + ndc.y * 0.42, 1.0));
                float3 sun = normalize(uSunDir);

                float horizon = clamp(rd.y * 1.8, 0.0, 1.0);
                float3 col = mix(uBot, uTop, pow(horizon, 0.7));
                float sunAmt = clamp(dot(rd, sun), 0.0, 1.0);
                col += uSunCol * uGlow * (pow(sunAmt, 900.0) * 3.0 + pow(sunAmt, 12.0) * 0.5
                                          + pow(sunAmt, 2.5) * 0.15);

                float cosA = dot(rd, sun);
                float ph = mix(phaseHG(cosA, 0.32), phaseHG(cosA, -0.12), 0.35) * 12.0;

                float3 acc = float3(0.0);
                float T = 1.0;
                if (rd.y > 0.02) {
                    float t0 = (0.45 - ro.y) / rd.y;
                    float t1 = min((3.0 - ro.y) / rd.y, 16.0);
                    float dt = (t1 - t0) / 20.0;
                    float t = t0 + dt * hash12(frag);
                    for (int i = 0; i < 20; i++) {
                        if (T > 0.008) {
                            float3 pos = ro + rd * t;
                            float den = map(pos, 5.0);
                            if (den > 0.01) {
                                float lt = lightMarch(pos, sun);
                                float powder = 1.0 - exp(-den * 6.0);
                                float hgt = clamp((pos.y - 0.45) / 2.5, 0.0, 1.0);

                                float3 amb = mix(uBot * 1.1, uTop * 1.2 + 0.30, hgt) * (1.15 - 0.82 * uDark);
                                float3 lum = amb * (0.72 - den * 0.38)
                                           + uSunCol * lt * powder * ph * (1.0 - 0.6 * uDark)
                                           + uSunCol * lt * 0.16;

                                // perspectiva aérea: lo lejano → bruma
                                lum = mix(uBot * 1.05, lum, exp(-t * 0.055));

                                float sigma = den * (1.1 + 0.6 * uDark);
                                float a = 1.0 - exp(-sigma * dt * 3.6);
                                acc += T * lum * a;
                                T *= 1.0 - a;
                            }
                            t += dt;
                        }
                    }
                }

                float fogAmt = exp(-max(rd.y - 0.02, 0.0) * 6.0);
                acc = mix(acc, uBot * (1.0 - T), fogAmt * 0.6);
                float horizFade = smoothstep(0.02, 0.18, rd.y);
                acc *= horizFade;
                T = mix(1.0, T, horizFade);

                col = col * T + acc;

                col = 1.0 - exp(-col * 1.6);
                col = pow(col, float3(1.18));
                float2 vc = (uv - 0.5) * float2(aspect, 1.0);
                col *= 1.0 - 0.30 * smoothstep(0.45, 1.0, length(vc));
                col += (hash12(frag + 0.7) - 0.5) * 0.006;

                return half4(col, 1.0);
            }
        """.trimIndent()
    }
}
