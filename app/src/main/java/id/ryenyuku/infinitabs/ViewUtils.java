package id.ryenyuku.infinitabs;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class ViewUtils {
    /**
     * Adjusts the view's margin depending on the system bar insets.
     *
     * @param view The specified view to adjust
     * @param adjustLeft Whether the left margin needs to be adjusted
     * @param adjustRight Whether the right margin needs to be adjusted
     * @param adjustTop Whether the top margin needs to be adjusted
     * @param adjustBottom Whether the bottom margin needs to be adjusted
     */
    public static void adjustPositionForSystemBarInsets(View view,
                                                       boolean adjustLeft,
                                                       boolean adjustRight,
                                                       boolean adjustTop,
                                                       boolean adjustBottom) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)v.getLayoutParams();

            if (adjustLeft) {
                mlp.leftMargin = insets.left;
            }
            if (adjustRight) {
                mlp.rightMargin = insets.right;
            }
            if (adjustTop) {
                mlp.topMargin = insets.top;
            }
            if (adjustBottom) {
                mlp.bottomMargin = insets.bottom;
            }

            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
