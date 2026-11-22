package com.nellecrzier.gofileexpressive.ui.glass

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Efek kaca HyperSpring dengan AGSL (Android 13+).
 *
 * Menerapkan resep dari `LIQUID_GLASS.md` pada lapisan konten yang sudah
 * diburamkan: saturasi 1,55, hue 1,1, highlight −8%, veil putih 3%, canto
 * (regangan tepi 3dp) dan garis cahaya 0,8dp yang mencampur 60% putih dengan
 * 40% warna tepat di luar tepi.
 *
 * Semua parameter dalan piksel agar sesuai kepadatan layar. Kalau shader gagal
 * dikompilasi di perangkat (AGSL berubah antar versi), [effect] mengembalikan
 * null dan pemanggil memakai blur biasa — aplikasi tidak boleh jatuh karena ini.
 */
object LiquidGlass {

    /** Benar bila perangkat bisa menjalankan shader kaca. */
    val supported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private var cachedShader: RuntimeShader? = null
    private var cachedFailed = false

    private const val AGSL = """
        uniform shader content;
        uniform float2 size;
        uniform float cornerRadius;
        uniform float edgeWidth;
        uniform float lightWidth;
        uniform float saturation;
        uniform float extraSaturation;
        uniform float highlights;

        // Jarak bertanda ke persegi panjang bersudut bulat.
        float sdRoundRect(float2 p, float2 halfSize, float r) {
            float2 q = abs(p) - halfSize + r;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        }

        // Matriks saturasi (luma Rec.709).
        half3 saturateColor(half3 c, float s) {
            half l = dot(c, half3(0.2126, 0.7152, 0.0722));
            return mix(half3(l), c, s);
        }

        // Pergeseran hue ringan ala HyperSpring (1,1 pada roda warna).
        half3 shiftHue(half3 c, float amount) {
            float angle = amount * 0.10;
            float cs = cos(angle);
            float sn = sin(angle);
            half3x3 m = half3x3(
                half3(0.299 + 0.701 * cs - 0.168 * sn, 0.587 - 0.587 * cs + 0.330 * sn, 0.114 - 0.114 * cs - 0.497 * sn),
                half3(0.299 - 0.299 * cs - 0.328 * sn, 0.587 + 0.413 * cs + 0.035 * sn, 0.114 - 0.114 * cs + 0.292 * sn),
                half3(0.299 - 0.300 * cs + 1.250 * sn, 0.587 - 0.588 * cs - 1.050 * sn, 0.114 + 0.886 * cs - 0.203 * sn)
            );
            return clamp(m * c, 0.0, 1.0);
        }

        half4 main(float2 coord) {
            float2 halfSize = size * 0.5;
            float2 p = coord - halfSize;
            float d = sdRoundRect(p, halfSize, cornerRadius);
            float2 normal = normalize(p + float2(0.0001, 0.0001));

            // 1. Interior: konten (sudah diburamkan), saturasi, hue, highlight.
            half4 base = content.eval(coord);
            half3 rgb = saturateColor(base.rgb, saturation);
            rgb = shiftHue(rgb, 1.0);
            rgb = rgb * (1.0 + highlights) + 0.03;

            // 2. Canto: regang 3dp ke luar, saturasi ekstra, tanpa pemutihan.
            float edge = smoothstep(-edgeWidth, 0.0, -d);
            if (d > -edgeWidth && d <= 0.0) {
                half4 stretched = content.eval(coord + normal * edgeWidth);
                half3 edgeColor = saturateColor(stretched.rgb, saturation + extraSaturation);
                rgb = mix(rgb, edgeColor, 0.55);
            }

            // 3. Garis cahaya 0,8dp: 60% putih + 40% warna di luar tepi,
            //    penuh di 1-2 piksel pertama lalu meluruh (smoothstep).
            float line = 1.0 - smoothstep(0.0, lightWidth, abs(d));
            if (line > 0.0) {
                half3 outside = content.eval(coord + normal * (lightWidth * 1.5)).rgb;
                half3 lit = mix(outside, half3(1.0), 0.60);
                rgb = mix(rgb, lit, line * 0.9);
            }

            // Di luar bentuk: transparan supaya sudut tetap bersih.
            float alpha = d <= 0.0 ? 1.0 : 0.0;
            return half4(rgb, alpha * base.a);
        }
    """

    /**
     * RenderEffect untuk lapisan konten: blur [blurRadiusPx], lalu shader kaca.
     * Null bila perangkat < 13 atau shader gagal disiapkan.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun effect(
        widthPx: Float,
        heightPx: Float,
        density: Float,
        cornerRadiusDp: Float = 37f,
        blurRadiusPx: Float = 26f,
    ): android.graphics.RenderEffect? {
        if (cachedFailed) return null
        return try {
            val shader = cachedShader ?: RuntimeShader(AGSL).also { cachedShader = it }
            shader.setFloatUniform("size", widthPx, heightPx)
            shader.setFloatUniform("cornerRadius", cornerRadiusDp * density)
            shader.setFloatUniform("edgeWidth", 3f * density)
            shader.setFloatUniform("lightWidth", 0.8f * density)
            shader.setFloatUniform("saturation", 1.55f)
            shader.setFloatUniform("extraSaturation", 0.4f)
            shader.setFloatUniform("highlights", -0.08f)

            val glass = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content")
            val blur = android.graphics.RenderEffect.createBlurEffect(
                blurRadiusPx,
                blurRadiusPx,
                android.graphics.Shader.TileMode.CLAMP,
            )
            // blur lebih dulu, lalu shader membaca hasilnya sebagai `content`.
            android.graphics.RenderEffect.createChainEffect(glass, blur)
        } catch (error: Throwable) {
            cachedFailed = true
            null
        }
    }
}

/** Versi Compose dari [LiquidGlass.effect]. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun liquidGlassEffect(
    widthPx: Float,
    heightPx: Float,
    density: Float,
    cornerRadiusDp: Float = 37f,
    blurRadiusPx: Float = 26f,
) = LiquidGlass.effect(widthPx, heightPx, density, cornerRadiusDp, blurRadiusPx)?.asComposeRenderEffect()
