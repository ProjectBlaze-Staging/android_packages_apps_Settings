/*
 * SPDX-FileCopyrightText: crDroid Android Project & The BlazeAOSP Project
 * SPDX-License-Identifier: GPL-3.0
 */

package org.evolution.settings.fragments.themes

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class UdfpsIconPicker : SettingsPreferenceFragment() {

    private var udfpsPkg = "org.evolution.udfps.icons"
    private var udfpsCtx: Context? = null
    private var udfpsRes: Resources? = null

    private val items = ArrayList<IconItem>(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.themes_udfps_icon_title)
        loadResources()
    }

    private fun loadResources() {
        items.clear()
        val host = activity ?: return

        try {
            val pkgCtx = host.createPackageContext(udfpsPkg, Context.CONTEXT_IGNORE_SECURITY)
            val res = pkgCtx.resources
            val arrayId = res.getIdentifier("udfps_icons", "array", udfpsPkg)
            if (arrayId != 0) {
                val iconNames = res.getStringArray(arrayId)
                udfpsCtx = pkgCtx
                udfpsRes = res
                iconNames.forEachIndexed { index, name ->
                    val id = res.getIdentifier(name, "drawable", udfpsPkg)
                    items.add(IconItem(index = index, name = name, drawableId = id))
                }
            }
        } catch (_: PackageManager.NameNotFoundException) {
            udfpsCtx = null
            udfpsRes = null
        }

        if (items.isEmpty()) {
            val builtInTitles = listOf(
                "Pixel Parmak İzi",
                "ColorOS 17 Halka",
                "iOS 27 Touch Kristal",
                "One UI 9 Ultrasonik",
                "Blaze Alev Çekirdeği",
                "Cyber Altıgen",
                "Luminous Pulsar",
                "Kuantum Dalga",
                "Minimal Nokta",
                "Yıldız Işığı",
                "Lotus Çiçeği",
                "Matrix Kalkanı"
            )
            builtInTitles.forEachIndexed { idx, title ->
                items.add(IconItem(index = idx, name = title, drawableId = 0))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_view, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.recycler_view)
        val spanCount = if (resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        ) 4 else 3
        rv.layoutManager = GridLayoutManager(requireContext(), spanCount)

        val adapter = UdfpsIconAdapter(
            context = requireContext(),
            contentResolver = requireContext().contentResolver,
            iconLoader = { item -> getIconDrawable(item) }
        )
        rv.adapter = adapter
        adapter.submitList(items.toList())
        return view
    }

    private fun getIconDrawable(item: IconItem): Drawable {
        val c = udfpsCtx
        if (c != null && item.drawableId != 0) {
            try {
                c.getDrawable(item.drawableId)?.let { return it }
            } catch (_: Resources.NotFoundException) {}
        }
        return BuiltInUdfpsIconDrawable(item.index)
    }

    override fun getMetricsCategory(): Int = MetricsEvent.VIEW_UNKNOWN

    companion object {
        @JvmStatic
        fun reset(context: Context) {
            Settings.System.putIntForUser(
                context.contentResolver,
                "udfps_icon",
                0,
                UserHandle.USER_CURRENT
            )
        }
    }

    private data class IconItem(
        val index: Int,
        val name: String,
        @DrawableRes val drawableId: Int
    )

    private class BuiltInUdfpsIconDrawable(private val styleIndex: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val palette = intArrayOf(
            0xFF38BDF8.toInt(), 0xFF818CF8.toInt(), 0xFFF43F5E.toInt(), 0xFF34D399.toInt(),
            0xFFFB923C.toInt(), 0xFFA78BFA.toInt(), 0xFF22D3EE.toInt(), 0xFFFBBF24.toInt(),
            0xFFE2E8F0.toInt(), 0xFF60A5FA.toInt(), 0xFFF472B6.toInt(), 0xFF4ADE80.toInt()
        )

        override fun draw(canvas: Canvas) {
            val b = bounds
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()
            val r = min(b.width(), b.height()) * 0.36f
            val color = palette[styleIndex % palette.size]

            // Subtle outer glow ring
            paint.style = Paint.Style.FILL
            paint.color = (color and 0x00FFFFFF) or 0x24000000
            canvas.drawCircle(cx, cy, r * 1.15f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = r * 0.11f
            paint.color = color

            when (styleIndex % 6) {
                0 -> {
                    // Concentric fingerprint ridges
                    canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.85f, cy + r * 0.85f), 200f, 140f, false, paint)
                    canvas.drawArc(RectF(cx - r * 0.58f, cy - r * 0.58f, cx + r * 0.58f, cy + r * 0.58f), 185f, 170f, false, paint)
                    canvas.drawArc(RectF(cx - r * 0.32f, cy - r * 0.32f, cx + r * 0.32f, cy + r * 0.32f), 160f, 220f, false, paint)
                    canvas.drawLine(cx, cy - r * 0.1f, cx, cy + r * 0.45f, paint)
                }
                1 -> {
                    // ColorOS 17 Double Luminous Ring
                    canvas.drawCircle(cx, cy, r * 0.85f, paint)
                    paint.strokeWidth = r * 0.06f
                    canvas.drawCircle(cx, cy, r * 0.52f, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, r * 0.18f, paint)
                }
                2 -> {
                    // Hexagon HUD
                    val path = Path()
                    for (i in 0..5) {
                        val a = Math.toRadians((i * 60 - 30).toDouble())
                        val px = cx + (r * 0.85f * cos(a)).toFloat()
                        val py = cy + (r * 0.85f * sin(a)).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                    canvas.drawCircle(cx, cy, r * 0.30f, paint)
                }
                3 -> {
                    // Ultrasonic Wave
                    for (i in 1..3) {
                        val rr = r * (0.28f * i)
                        canvas.drawArc(RectF(cx - rr, cy - rr, cx + rr, cy + rr), -60f, 120f, false, paint)
                        canvas.drawArc(RectF(cx - rr, cy - rr, cx + rr, cy + rr), 120f, 120f, false, paint)
                    }
                }
                4 -> {
                    // Starburst / Lotus
                    for (i in 0..7) {
                        val a = Math.toRadians((i * 45).toDouble())
                        val x1 = cx + (r * 0.25f * cos(a)).toFloat()
                        val y1 = cy + (r * 0.25f * sin(a)).toFloat()
                        val x2 = cx + (r * 0.85f * cos(a)).toFloat()
                        val y2 = cy + (r * 0.85f * sin(a)).toFloat()
                        canvas.drawLine(x1, y1, x2, y2, paint)
                    }
                }
                else -> {
                    // Rounded Diamond Shield
                    val p = Path()
                    p.moveTo(cx, cy - r * 0.88f)
                    p.lineTo(cx + r * 0.88f, cy)
                    p.lineTo(cx, cy + r * 0.88f)
                    p.lineTo(cx - r * 0.88f, cy)
                    p.close()
                    canvas.drawPath(p, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx, cy, r * 0.22f, paint)
                }
            }
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private class UdfpsIconAdapter(
        private val context: Context,
        private val contentResolver: ContentResolver,
        private val iconLoader: (IconItem) -> Drawable
    ) : ListAdapter<IconItem, UdfpsIconAdapter.VH>(DIFF) {

        private var selectedPos: Int = -1

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long = getItem(position).index.toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_option, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            if (selectedPos == -1) {
                selectedPos = Settings.System.getIntForUser(
                    contentResolver, "udfps_icon", 0, UserHandle.USER_CURRENT
                )
            }

            holder.image.setImageDrawable(iconLoader(item))
            holder.image.setPadding(16, 16, 16, 16)
            holder.name.visibility = View.VISIBLE
            holder.name.text = item.name
            holder.itemView.isActivated = (position == selectedPos)

            holder.itemView.setOnClickListener {
                val old = selectedPos
                selectedPos = position
                if (old != -1) notifyItemChanged(old)
                notifyItemChanged(selectedPos)

                Settings.System.putIntForUser(
                    contentResolver, "udfps_icon", position, UserHandle.USER_CURRENT
                )
                Toast.makeText(context, "UDFPS ikonu seçildi: ${item.name}", Toast.LENGTH_SHORT).show()
            }
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
            val image: ImageView = itemView.findViewById(R.id.option_thumbnail)
        }

        private companion object {
            val DIFF = object : DiffUtil.ItemCallback<IconItem>() {
                override fun areItemsTheSame(oldItem: IconItem, newItem: IconItem): Boolean =
                    oldItem.index == newItem.index
                override fun areContentsTheSame(oldItem: IconItem, newItem: IconItem): Boolean =
                    oldItem == newItem
            }
        }
    }
}
