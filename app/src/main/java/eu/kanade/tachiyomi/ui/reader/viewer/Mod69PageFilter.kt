package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.View

// KMK -->
/**
 * 69 mod R2: GPU picture filters on reader pages.
 * "sharpen" / "strong": unsharp mask in a GPU shader (Android 13+).
 * "smooth": a light blur that hides blocky low-quality pages (Android 12+).
 */
object Mod69PageFilter {

    private const val SHARPEN = """
        uniform shader content;
        uniform float amount;
        half4 main(float2 p) {
            half4 c = content.eval(p);
            half4 n = content.eval(p + float2(0, -1)) + content.eval(p + float2(0, 1)) +
                content.eval(p + float2(-1, 0)) + content.eval(p + float2(1, 0));
            half4 s = c + (c * 4.0 - n) * amount;
            return half4(clamp(s.rgb, 0.0, 1.0), c.a);
        }
    """

    fun apply(view: View, mode: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        view.setRenderEffect(
            when (mode) {
                "sharpen", "strong" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val shader = RuntimeShader(SHARPEN)
                    shader.setFloatUniform("amount", if (mode == "strong") 0.6f else 0.3f)
                    RenderEffect.createRuntimeShaderEffect(shader, "content")
                } else {
                    null
                }
                "smooth" -> RenderEffect.createBlurEffect(0.9f, 0.9f, Shader.TileMode.CLAMP)
                else -> null
            },
        )
    }
}
// KMK <--
