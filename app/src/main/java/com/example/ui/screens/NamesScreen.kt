package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.catalog.RegionalNameCatalog
import com.example.ui.viewmodel.DisplayNameItem
import com.example.ui.viewmodel.PakGivesViewModel

@Composable
fun NamesScreen(
  viewModel: PakGivesViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
  val selectedGender by viewModel.selectedGender.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val favoritesOnly by viewModel.favoritesOnly.collectAsStateWithLifecycle()
  val spotlight by viewModel.spotlightName.collectAsStateWithLifecycle()
  val names by viewModel.filteredNames.collectAsStateWithLifecycle()
  val favoriteIds by viewModel.favoriteNameIds.collectAsStateWithLifecycle()
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

  var showSubmitDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showSubmitDialog = true },
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = Modifier.testTag("add_name_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_submit_name))
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Hero Banner
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(RoundedCornerShape(22.dp)),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_names),
            contentDescription = stringResource(R.string.cd_hero_banner),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(Color(0xD9052417), Color(0x66052417))
                )
              )
          )
          Column(
            modifier = Modifier
              .align(Alignment.CenterStart)
              .padding(18.dp),
          ) {
            Text(
              text = stringResource(R.string.names_hero_title),
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = stringResource(R.string.names_hero_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFDDF3E8),
            )
          }
        }
      }

      // Spotlight Suggester Card
      item {
        SpotlightSuggesterCard(
          spotlight = spotlight,
          isFavorite = spotlight?.let { favoriteIds.contains(it.id) } ?: false,
          onInspireClick = { viewModel.suggestRandomName() },
          onFavoriteClick = { spotlight?.let { viewModel.toggleFavorite(it) } },
          onShareClick = {
            spotlight?.let { item ->
              val shareText = "${item.name} (${item.script}) — ${item.originRegion} ${item.gender} name meaning: ${item.meaning}. Shared via PakGives."
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
              }
              context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_name_title)))
            }
          },
        )
      }

      // Search & Favorites Toggle
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text(stringResource(R.string.search_names_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("names_search_input"),
          )
          FilterChip(
            selected = favoritesOnly,
            onClick = { viewModel.toggleFavoritesOnly() },
            label = { Text(stringResource(R.string.saved_chip)) },
            leadingIcon = {
              Icon(
                imageVector = if (favoritesOnly) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.testTag("favorites_filter_chip"),
          )
        }
      }

      // Regional Origins Horizontal Selector
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = stringResource(R.string.regional_origin_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(RegionalNameCatalog.regions) { region ->
              FilterChip(
                selected = selectedRegion == region,
                onClick = { viewModel.selectRegion(region) },
                label = { Text(region) },
                leadingIcon = if (selectedRegion == region) {
                  { Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
              )
            }
          }
        }
      }

      // Gender Filter Chips
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(RegionalNameCatalog.genders) { gender ->
            FilterChip(
              selected = selectedGender == gender,
              onClick = { viewModel.selectGender(gender) },
              label = { Text(gender) },
            )
          }
        }
      }

      if (names.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = stringResource(R.string.empty_names_title),
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = stringResource(R.string.empty_names_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(names, key = { it.id }) { item ->
          RegionalNameCard(
            item = item,
            isFavorite = favoriteIds.contains(item.id),
            canDelete = item.isCommunity && (isAdmin || item.authorUserId == viewModel.currentUserId),
            onToggleFavorite = { viewModel.toggleFavorite(item) },
            onLike = { if (item.isCommunity) viewModel.likeCommunityName(item.id) },
            onDelete = { viewModel.deleteCommunityName(item.id) },
          )
        }
      }

      item { Spacer(modifier = Modifier.height(72.dp)) }
    }
  }

  if (showSubmitDialog) {
    SubmitRegionalNameDialog(
      defaultRegion = if (selectedRegion == "All Regions") "Punjabi" else selectedRegion,
      onDismiss = { showSubmitDialog = false },
      onSubmit = { name, script, pronunciation, region, gender, meaning, note ->
        viewModel.submitRegionalName(name, script, pronunciation, region, gender, meaning, note)
        showSubmitDialog = false
      },
    )
  }
}

@Composable
private fun SpotlightSuggesterCard(
  spotlight: DisplayNameItem?,
  isFavorite: Boolean,
  onInspireClick: () -> Unit,
  onFavoriteClick: () -> Unit,
  onShareClick: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ),
    shape = RoundedCornerShape(22.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = RoundedCornerShape(50),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.spotlight_badge),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          }
        }
        Button(
          onClick = onInspireClick,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("suggest_name_button"),
        ) {
          Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.suggest_another))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      AnimatedContent(targetState = spotlight, label = "spotlight_anim") { item ->
        if (item == null) {
          Text(stringResource(R.string.loading_suggestion), style = MaterialTheme.typography.bodyMedium)
        } else {
          Column {
            Row(
              verticalAlignment = Alignment.Bottom,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.headlineMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (item.script.isNotBlank() || item.pronunciation.isNotBlank()) {
                  Text(
                    text = listOfNotNull(
                      item.script.takeIf { it.isNotBlank() },
                      item.pronunciation.takeIf { it.isNotBlank() }?.let { "($it)" }
                    ).joinToString("  •  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                  )
                }
              }
              Row {
                IconButton(onClick = onFavoriteClick) {
                  Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.cd_favorite_name),
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                IconButton(onClick = onShareClick) {
                  Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.cd_share_name),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = item.meaning,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.culturalNote.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = item.culturalNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OriginPill(text = item.originRegion)
              OriginPill(text = item.gender)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RegionalNameCard(
  item: DisplayNameItem,
  isFavorite: Boolean,
  canDelete: Boolean,
  onToggleFavorite: () -> Unit,
  onLike: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = item.name,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.script.isNotBlank()) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = item.script,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
          if (item.pronunciation.isNotBlank()) {
            Text(
              text = "Pronounced: ${item.pronunciation}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (item.isCommunity) {
            TextButton(onClick = onLike) {
              Icon(Icons.Filled.ThumbUp, contentDescription = stringResource(R.string.cd_like_name), modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("${item.likesCount}")
            }
          }
          IconButton(onClick = onToggleFavorite) {
            Icon(
              imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
              contentDescription = stringResource(R.string.cd_favorite_name),
              tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (canDelete) {
            IconButton(onClick = onDelete) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = stringResource(R.string.cd_delete_name),
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = item.meaning,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (item.culturalNote.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.culturalNote,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OriginPill(text = item.originRegion)
        OriginPill(text = item.gender)
        if (item.isCommunity) {
          OriginPill(text = "By ${item.authorName}", isAccent = true)
        }
      }
    }
  }
}

@Composable
fun OriginPill(
  text: String,
  isAccent: Boolean = false,
) {
  Surface(
    color = if (isAccent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(50),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = if (isAccent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun SubmitRegionalNameDialog(
  defaultRegion: String,
  onDismiss: () -> Unit,
  onSubmit: (String, String, String, String, String, String, String) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var script by remember { mutableStateOf("") }
  var pronunciation by remember { mutableStateOf("") }
  var region by remember { mutableStateOf(defaultRegion) }
  var gender by remember { mutableStateOf("Boy") }
  var meaning by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }

  val availableRegions = RegionalNameCatalog.regions.drop(1)
  val availableGenders = RegionalNameCatalog.genders.drop(1)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.submit_name_dialog_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.name_field_label)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("submit_name_input"),
        )
        OutlinedTextField(
          value = script,
          onValueChange = { script = it },
          label = { Text(stringResource(R.string.script_field_label)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = pronunciation,
          onValueChange = { pronunciation = it },
          label = { Text(stringResource(R.string.pronunciation_field_label)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.regional_origin_label), style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(availableRegions) { item ->
            FilterChip(
              selected = region == item,
              onClick = { region = item },
              label = { Text(item) },
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          availableGenders.forEach { g ->
            FilterChip(
              selected = gender == g,
              onClick = { gender = g },
              label = { Text(g) },
            )
          }
        }
        OutlinedTextField(
          value = meaning,
          onValueChange = { meaning = it },
          label = { Text(stringResource(R.string.meaning_field_label)) },
          modifier = Modifier.fillMaxWidth().testTag("submit_meaning_input"),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text(stringResource(R.string.cultural_note_label)) },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(name, script, pronunciation, region, gender, meaning, note) },
        enabled = name.isNotBlank() && meaning.trim().length >= 2,
        modifier = Modifier.testTag("confirm_submit_name_button"),
      ) {
        Text(stringResource(R.string.submit_action))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel_action))
      }
    },
  )
}
