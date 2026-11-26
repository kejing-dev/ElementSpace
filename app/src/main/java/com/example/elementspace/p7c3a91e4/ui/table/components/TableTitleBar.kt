package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.domain.model.LearningLevel
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SearchField
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.ToggleButton
import com.pico.spatial.ui.design.TitleBar
import com.pico.spatial.ui.design.menu.Menu
import com.pico.spatial.ui.design.menu.MenuItem

/** Title, app-wide search and the settings menu. */
@Composable
fun TableTitleBar(
    query: String,
    onQueryChange: (String) -> Unit,
    language: AppLanguage,
    languageMenuOpen: Boolean,
    onLanguageMenuVisible: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onHowTo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    collideMode: Boolean = false,
    onCollideToggle: () -> Unit = {},
    level: LearningLevel = LearningLevel.BASIC,
    onLevelSelected: (LearningLevel) -> Unit = {},
    onGuide: () -> Unit = {},
) {
    TitleBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_atom),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Text(str(R.string.table_title), maxLines = 1)
            }
        },
        trailingActions = {
            Button(onClick = onGuide, modifier = Modifier.focusProperties { canFocus = false }) { Text(str(R.string.guide), maxLines = 1) }
            ToggleButton(checked = collideMode, onCheckedChange = { onCollideToggle() }, modifier = Modifier.focusProperties { canFocus = false }) {
                Text(str(R.string.collide), maxLines = 1)
            }
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                onSearch = onQueryChange,
                modifier = Modifier.width(420.dp),
                placeholder = { Text(str(R.string.search_placeholder), style = PicoTheme.typography.bodyLarge, maxLines = 1) },
            )
            SettingsMenu(
                language = language,
                open = languageMenuOpen,
                onVisible = onLanguageMenuVisible,
                onSelected = onLanguageSelected,
                onHowTo = onHowTo,
                onExit = onExit,
                level = level,
                onLevelSelected = onLevelSelected,
            )
        },
    )
}

/**
 * Settings (profile D26): the two languages, "How to use" and "Exit app". The language picker used
 * to be its own dropdown; it moved in here so the title bar keeps one entry for everything.
 */
@Composable
private fun SettingsMenu(
    language: AppLanguage,
    open: Boolean,
    onVisible: (Boolean) -> Unit,
    onSelected: (AppLanguage) -> Unit,
    onHowTo: () -> Unit,
    onExit: () -> Unit,
    level: LearningLevel,
    onLevelSelected: (LearningLevel) -> Unit,
) {
    Box {
        Button(onClick = { onVisible(!open) }, modifier = Modifier.focusProperties { canFocus = false }) {
            Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(str(R.string.settings), maxLines = 1)
        }
        if (open) {
            Menu(onDismissRequest = { onVisible(false) }) {
                AppLanguage.entries.forEach { option ->
                    MenuItem(
                        title = { Text(option.nativeName) },
                        onClick = { onSelected(option) },
                        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_globe), contentDescription = null, modifier = Modifier.size(24.dp)) },
                        trailingIcon = {
                            if (option == language) {
                                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        },
                    )
                }
                // How much the cards show (v1.7.0): beginner / school / university.
                LearningLevel.entries.forEach { option ->
                    MenuItem(
                        title = { Text(str(R.string.level_item, if (language == AppLanguage.ZH) option.zh else option.en)) },
                        onClick = { onLevelSelected(option) },
                        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_atom), contentDescription = null, modifier = Modifier.size(24.dp)) },
                        trailingIcon = {
                            if (option == level) {
                                Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        },
                    )
                }
                MenuItem(
                    title = { Text(str(R.string.how_to_title)) },
                    onClick = onHowTo,
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_help), contentDescription = null, modifier = Modifier.size(24.dp)) },
                )
                MenuItem(
                    title = { Text(str(R.string.exit_app)) },
                    onClick = onExit,
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_exit), contentDescription = null, modifier = Modifier.size(24.dp)) },
                )
            }
        }
    }
}
