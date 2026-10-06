package com.example.elementspace.p7c3a91e4.ui.table.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.elementspace.p7c3a91e4.R
import com.example.elementspace.p7c3a91e4.domain.model.AppLanguage
import com.example.elementspace.p7c3a91e4.ui.theme.str
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Icon
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SearchField
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TitleBar
import com.pico.spatial.ui.design.menu.Menu
import com.pico.spatial.ui.design.menu.MenuItem

/** Title, app-wide search and the language dropdown. */
@Composable
fun TableTitleBar(
    query: String,
    onQueryChange: (String) -> Unit,
    language: AppLanguage,
    languageMenuOpen: Boolean,
    onLanguageMenuVisible: (Boolean) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
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
            SearchField(
                value = query,
                onValueChange = onQueryChange,
                onSearch = onQueryChange,
                modifier = Modifier.width(420.dp),
                placeholder = { Text(str(R.string.search_placeholder), style = PicoTheme.typography.bodyLarge, maxLines = 1) },
            )
            LanguageDropdown(
                language = language,
                open = languageMenuOpen,
                onVisible = onLanguageMenuVisible,
                onSelected = onLanguageSelected,
            )
        },
    )
}

@Composable
private fun LanguageDropdown(
    language: AppLanguage,
    open: Boolean,
    onVisible: (Boolean) -> Unit,
    onSelected: (AppLanguage) -> Unit,
) {
    Box {
        Button(onClick = { onVisible(!open) }) {
            Icon(
                painter = painterResource(R.drawable.ic_globe),
                contentDescription = str(R.string.language),
                modifier = Modifier.size(24.dp),
            )
            Text(language.nativeName, maxLines = 1)
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        if (open) {
            Menu(onDismissRequest = { onVisible(false) }) {
                AppLanguage.entries.forEach { option ->
                    MenuItem(
                        title = { Text(option.nativeName) },
                        onClick = { onSelected(option) },
                        trailingIcon = {
                            if (option == language) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
