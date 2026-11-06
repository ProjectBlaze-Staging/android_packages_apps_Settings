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
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.AnimationDrawable
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

class UdfpsAnimation : SettingsPreferenceFragment() {

    private var udfpsPkg = "org.evolution.udfps.animations"
    private var udfpsRes: Resources? = null
    private val items = ArrayList<AnimItem>(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.themes_udfps_animation_title)
        loadResources()
    }

    private fun loadResources() {
        items.clear()
        val host = activity ?: return

        udfpsRes = try {
            host.packageManager.getResourcesForApplication(udfpsPkg)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        val res = udfpsRes
        if (res != null) {
            val stylesId = res.getIdentifier("udfps_animation_styles", "array", udfpsPkg)
            val previewsId = res.getIdentifier("udfps_animation_previews", "array", udfpsPkg)
            val titlesId = res.getIdentifier("udfps_animation_titles", "array", udfpsPkg)
            if (stylesId != 0 && previewsId != 0 && titlesId != 0) {
                val styles = res.getStringArray(stylesId)
                val previews = res.getStringArray(previewsId)
                val titles = res.getStringArray(titlesId)
                val count = minOf(styles.size, previews.size, titles.size)
                for (i in 0 until count) {
                    items.add(AnimItem(i, styles[i], previews[i], titles[i]))
                }
            }
        }

        if (items.isEmpty()) {
            val builtIn = listOf(
                "Yok (Standart)",
                "ColorOS 17 Aquamorphic Dalga",
                "Luminous Aurora Halkası",
                "iOS 27 Liquid Glass Parıltı",
                "One UI 9 Yıldız Tozu",
                "Blaze Süpernova Ateşi",
                "Cyberpunk Neon Nabız",
                "Kuantum Vortex"
            )
            builtIn.forEachIndexed { idx, title ->
                items.add(AnimItem(idx, "builtin_$idx", "builtin_$idx", title))
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

        val adapter = UdfpsAnimAdapter(
            context = requireContext(),
            contentResolver = requireContext().contentResolver,
            drawableLoader = { item -> getDrawableForItem(item) }
        )
        rv.adapter = adapter
        adapter.submitList(items.toList())
        return view
    }

    override fun getMetricsCategory(): Int = MetricsEvent.VIEW_UNKNOWN

    private fun getDrawableForItem(item: AnimItem): Drawable {
        val res = udfpsRes
        if (res != null && !item.previewDrawableName.startsWith("builtin_")) {
            val id = res.getIdentifier(item.previewDrawableName, "drawable", udfpsPkg)
            if (id != 0) {
                try {
                    res.getDrawable(id, requireContext().theme)?.let { return it }
                } catch (_: Resources.NotFoundException) {}
            }
        }
        return BuiltInUdfpsAnimDrawable(item.index)
    }

    companion object {
        @JvmStatic
        fun reset(context: Context) {
            Settings.System.putIntForUser(
                context.contentResolver,
                "udfps_anim_style",
                0,
                UserHandle.USER_CURRENT
            )
        }
    }

    private data class AnimItem(
        val index: Int,
        val animDrawableName: String,
        val previewDrawableName: String,
        val title: String
    )

    private class BuiltInUdfpsAnimDrawable(private val styleIndex: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val colors = intArrayOf(
            0xFF94A3B8.toInt(), 0xFF38BDF8.toInt(), 0xFF818CF8.toInt(), 0xFFE2E8F0.toInt(),
            0xFFA78BFA.toInt(), 0xFFFB923C.toInt(), 0xFF22D3EE.toInt(), 0xFF34D399.toInt()
        )

        override fun draw(canvas: Canvas) {
            val b = bounds
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()
            val r = min(b.width(), b.height()) * 0.42f
            if (r <= 0f) return
            val c = colors[styleIndex % colors.size]

            paint.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(c, (c and 0x00FFFFFF) or 0x55000000, 0x00000000),
                floatArrayOf(0.15f, 0.65f, 1.0f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.color = c
            paint.strokeWidth = r * 0.06f
            canvas.drawCircle(cx, cy, r * 0.55f, paint)

            val rays = 8 + (styleIndex % 4) * 2
            for (i in 0 until rays) {
                val a = Math.toRadians((i * (360 / rays)).toDouble())
                val x1 = cx + (r * 0.60f * cos(a)).toFloat()
                val y1 = cy + (r * 0.60f * sin(a)).toFloat()
                val x2 = cx + (r * 0.90f * cos(a)).toFloat()
                val y2 = cy + (r * 0.90f * sin(a)).toFloat()
                canvas.drawLine(x1, y1, x2, y2, paint)
            }
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    private class UdfpsAnimAdapter(
        private val context: Context,
        private val contentResolver: ContentResolver,
        private val drawableLoader: (AnimItem) -> Drawable
    ) : ListAdapter<AnimItem, UdfpsAnimAdapter.VH>(DIFF) {

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
                    contentResolver, "udfps_anim_style", 0, UserHandle.USER_CURRENT
                )
            }

            holder.image.setImageDrawable(drawableLoader(item))
            holder.name.text = item.title
            holder.itemView.isActivated = (position == selectedPos)

            holder.itemView.setOnClickListener {
                val old = selectedPos
                selectedPos = position
                if (old != -1) notifyItemChanged(old)
                notifyItemChanged(selectedPos)

                holder.image.animate()
                    .scaleX(1.22f).scaleY(1.22f).setDuration(180)
                    .withEndAction {
                        holder.image.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
                    }.start()

                Settings.System.putIntForUser(
                    contentResolver, "udfps_anim_style", position, UserHandle.USER_CURRENT
                )
                Toast.makeText(context, "UDFPS animasyonu seçildi: ${item.title}", Toast.LENGTH_SHORT).show()
            }
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.option_label)
            val image: ImageView = itemView.findViewById(R.id.option_thumbnail)
        }

        private companion object {
            val DIFF = object : DiffUtil.ItemCallback<AnimItem>() {
                override fun areItemsTheSame(oldItem: AnimItem, newItem: AnimItem): Boolean =
                    oldItem.index == newItem.index
                override fun areContentsTheSame(oldItem: AnimItem, newItem: AnimItem): Boolean =
                    oldItem == newItem
            }
        }
    }
}
