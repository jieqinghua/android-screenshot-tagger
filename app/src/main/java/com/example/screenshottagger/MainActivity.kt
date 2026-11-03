package com.example.screenshottagger

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 原生 View 首页：不加载 AndroidX/Compose，关掉后不给常驻进程留负担。
 * 三种状态共用一个布局：首次设置、正常工作（权限折叠）、权限异常（权限自动展开）。
 */
class MainActivity : Activity() {
    private enum class Mode { SETUP, WORKING, ERROR }

    private class Row(val root: View, val sub: TextView, val action: View, val check: View, val subRes: Int)

    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var hero: View
    private lateinit var heroImage: ImageView
    private lateinit var heroTitle: TextView
    private lateinit var heroSub: TextView
    private lateinit var setupLabel: View
    private lateinit var setupProgress: TextView
    private lateinit var permissionCard: View
    private lateinit var header: View
    private lateinit var headerIcon: ImageView
    private lateinit var summary: TextView
    private lateinit var chevron: View
    private lateinit var rows: List<Row>
    private lateinit var dividers: List<View>
    private lateinit var hintCard: View
    private lateinit var logSection: View
    private lateinit var logList: LinearLayout
    private lateinit var logEmpty: View
    private lateinit var privacy: View
    private lateinit var privacySpace: View

    private var mode: Mode? = null
    private var expanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scroll = findViewById(R.id.scroll)
        content = findViewById(R.id.content)
        hero = findViewById(R.id.hero)
        heroImage = findViewById(R.id.hero_image)
        heroTitle = findViewById(R.id.hero_title)
        heroSub = findViewById(R.id.hero_sub)
        setupLabel = findViewById(R.id.setup_label)
        setupProgress = findViewById(R.id.setup_progress)
        permissionCard = findViewById(R.id.permission_card)
        header = findViewById(R.id.permission_header)
        headerIcon = findViewById(R.id.permission_header_icon)
        summary = findViewById(R.id.permission_summary)
        chevron = findViewById(R.id.permission_chevron)
        hintCard = findViewById(R.id.hint_card)
        logSection = findViewById(R.id.log_section)
        logList = findViewById(R.id.log_list)
        logEmpty = findViewById(R.id.log_empty)
        privacy = findViewById(R.id.privacy)
        privacySpace = findViewById(R.id.privacy_space)
        dividers = listOf(
            findViewById(R.id.divider_header), findViewById(R.id.divider_files), findViewById(R.id.divider_battery),
        )

        rows = listOf(
            row(R.id.row_a11y, R.drawable.ic_a11y, R.string.a11y_title, R.string.a11y_sub) {
                // 有 WRITE_SECURE_SETTINGS 时直接开启，否则跳系统设置
                if (A11yState.tryEnable(this)) refresh() else open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            row(R.id.row_files, R.drawable.ic_folder, R.string.files_title, R.string.files_sub) {
                open(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, packageUri()))
            },
            row(R.id.row_battery, R.drawable.ic_battery, R.string.battery_title, R.string.battery_sub) {
                open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()))
            },
        )
        header.setOnClickListener {
            expanded = !expanded
            bindExpanded()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun row(id: Int, icon: Int, title: Int, sub: Int, onOpen: () -> Unit): Row {
        val root = findViewById<View>(id)
        root.findViewById<ImageView>(R.id.icon).setImageResource(icon)
        root.findViewById<TextView>(R.id.title).setText(title)
        val action = root.findViewById<View>(R.id.action)
        root.setOnClickListener { onOpen() }
        action.setOnClickListener { onOpen() }
        return Row(root, root.findViewById(R.id.sub), action, root.findViewById(R.id.check), sub)
    }

    private fun refresh() {
        val granted = listOf(
            A11yState.isEnabled(this),
            Environment.isExternalStorageManager(),
            getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
        )
        val missing = granted.count { !it }
        val newMode = when {
            missing == 0 -> Mode.WORKING
            Prefs.wasActivated(this) -> Mode.ERROR
            else -> Mode.SETUP
        }
        // 从设置页回来、三项刚好凑齐时播放激活转场
        val activating = newMode == Mode.WORKING && mode != null && mode != Mode.WORKING
        if (newMode == Mode.WORKING) Prefs.setActivated(this)
        if (newMode != mode) expanded = newMode == Mode.ERROR
        mode = newMode

        resetAnimation()
        bindHero(newMode)
        bindPermissions(newMode, granted, missing)
        hintCard.visibility = if (newMode == Mode.WORKING) View.GONE else View.VISIBLE
        logSection.visibility = if (newMode == Mode.SETUP) View.GONE else View.VISIBLE
        privacy.visibility = if (newMode == Mode.SETUP) View.VISIBLE else View.GONE
        privacySpace.visibility = privacy.visibility
        if (newMode != Mode.SETUP) bindLog()
        if (activating && ValueAnimator.areAnimatorsEnabled()) playActivation()
    }

    private fun bindHero(mode: Mode) {
        val setup = mode == Mode.SETUP
        content.setPadding(content.paddingLeft, dp(if (setup) 56 else 36), content.paddingRight, content.paddingBottom)
        hero.setPadding(0, 0, 0, dp(if (setup) 22 else 14))
        heroImage.layoutParams = heroImage.layoutParams.apply {
            width = dp(if (setup) 200 else 168)
            height = dp(if (setup) 150 else 126)
        }
        heroImage.setImageResource(
            when (mode) {
                Mode.SETUP -> R.drawable.illo_default
                Mode.WORKING -> R.drawable.illo_active
                Mode.ERROR -> R.drawable.illo_inactive
            }
        )
        heroTitle.setTextAppearance(if (setup) R.style.Text_Display else R.style.Text_Title)
        heroTitle.setText(
            when (mode) {
                Mode.SETUP -> R.string.app_name
                Mode.WORKING -> R.string.title_active
                Mode.ERROR -> R.string.title_inactive
            }
        )
        heroSub.setText(
            when (mode) {
                Mode.SETUP -> R.string.tagline
                Mode.WORKING -> R.string.sub_active
                Mode.ERROR -> R.string.sub_inactive
            }
        )
    }

    private fun bindPermissions(mode: Mode, granted: List<Boolean>, missing: Int) {
        val setup = mode == Mode.SETUP
        setupLabel.visibility = if (setup) View.VISIBLE else View.GONE
        setupProgress.text = getString(R.string.permissions_progress, granted.size - missing)
        header.visibility = if (setup) View.GONE else View.VISIBLE
        headerIcon.setImageResource(if (missing == 0) R.drawable.ic_shield_ok else R.drawable.ic_shield_alert)
        summary.text = if (missing == 0) getString(R.string.permissions_all_on)
        else getString(R.string.permissions_missing, missing)
        summary.setTextColor(getColor(if (missing == 0) R.color.success else R.color.danger))

        rows.forEachIndexed { i, row ->
            val ok = granted[i]
            row.check.visibility = if (ok) View.VISIBLE else View.GONE
            row.action.visibility = if (ok) View.GONE else View.VISIBLE
            val revoked = !ok && mode == Mode.ERROR
            row.sub.setText(if (revoked) R.string.permission_revoked else row.subRes)
            row.sub.setTextColor(getColor(if (revoked) R.color.danger else R.color.ink_muted))
        }
        bindExpanded()
    }

    private fun bindExpanded() {
        val setup = mode == Mode.SETUP
        val showRows = setup || expanded
        rows.forEach { it.root.visibility = if (showRows) View.VISIBLE else View.GONE }
        dividers.forEachIndexed { i, divider ->
            // 第一条分隔线在折叠头下方，首次设置页没有折叠头
            val visible = showRows && !(i == 0 && setup)
            divider.visibility = if (visible) View.VISIBLE else View.GONE
        }
        chevron.rotation = if (expanded) 180f else 0f
        header.contentDescription = getString(R.string.title_permissions) + "，" + summary.text + "，" +
            getString(if (expanded) R.string.collapse else R.string.expand)
    }

    private fun bindLog() {
        logList.removeAllViews()
        val lines = RenameLog.read(this).map(LogLine::parse)
        logList.visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE
        logEmpty.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE
        val today = LocalDate.now()
        lines.forEachIndexed { i, line ->
            if (i > 0) {
                logList.addView(View(this).apply { setBackgroundColor(getColor(R.color.line)) },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
            }
            val item = layoutInflater.inflate(R.layout.item_log, logList, false)
            item.findViewById<TextView>(R.id.name).text = highlighted(line)
            val time = item.findViewById<TextView>(R.id.time)
            val label = line.time?.let { t ->
                val clock = t.format(CLOCK)
                when (t.toLocalDate()) {
                    today -> clock
                    today.minusDays(1) -> getString(R.string.time_yesterday, clock)
                    else -> t.format(DATE)
                }
            }
            time.text = label
            time.visibility = if (label == null) View.GONE else View.VISIBLE
            logList.addView(item)
        }
    }

    private fun highlighted(line: LogLine): CharSequence {
        if (!line.hasTag) return line.newName
        return SpannableString(line.newName).apply {
            setSpan(BackgroundColorSpan(getColor(R.color.marker_soft)), line.tagStart, line.tagEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(getColor(R.color.on_marker_soft)), line.tagStart, line.tagEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** 激活转场：插画在屏幕中央弹出放大，停顿后缩回顶部，其余内容从下方浮入。 */
    private fun playActivation() {
        val rest = listOf(permissionCard, logSection)
        scroll.scrollTo(0, 0)
        hero.alpha = 0f
        heroSub.alpha = 0f
        rest.forEach { it.alpha = 0f }
        hero.post {
            hero.pivotX = hero.width / 2f
            hero.pivotY = heroImage.top + heroImage.height / 2f
            hero.translationY = scroll.height / 2f - (hero.top + hero.pivotY)
            hero.scaleX = 0.6f
            hero.scaleY = 0.6f
            hero.animate().alpha(1f).scaleX(1.3f).scaleY(1.3f).setStartDelay(150).setDuration(450)
                .setInterpolator(OvershootInterpolator()).withEndAction {
                    hero.animate().translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(900).setDuration(600)
                        .setInterpolator(DecelerateInterpolator()).withEndAction {
                            heroSub.animate().alpha(1f).setStartDelay(0).setDuration(300)
                            rest.forEach { v ->
                                v.translationY = dp(32).toFloat()
                                v.animate().alpha(1f).translationY(0f).setStartDelay(0).setDuration(400)
                                    .setInterpolator(DecelerateInterpolator())
                            }
                        }
                }
        }
    }

    private fun resetAnimation() {
        listOf(hero, heroSub, permissionCard, logSection).forEach { v ->
            v.animate().cancel()
            v.alpha = 1f
            v.translationY = 0f
            v.scaleX = 1f
            v.scaleY = 1f
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun packageUri() = Uri.parse("package:$packageName")

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))
        }
    }

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    }
}
