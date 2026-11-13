package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.PakGivesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.BuyCoinsScreen
import com.example.ui.screens.DonationsScreen
import com.example.ui.screens.GiveawaysScreen
import com.example.ui.screens.NamesScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TermsPoliciesScreen
import com.example.ui.theme.PakGivesTheme
import com.example.ui.viewmodel.PakGivesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_NAMES = "names"
const val ROUTE_GIVEAWAYS = "giveaways"
const val ROUTE_DONATE = "donate"
const val ROUTE_BUY_COINS = "buy_coins"
const val ROUTE_PROFILE = "profile"
const val ROUTE_TERMS = "terms"
const val ROUTE_ADMIN = "admin"

private data class NavDestinationItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PakGivesTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      MainFeatureScreen(
        currentUserId = user.uid,
        currentUserName = user.displayName ?: "PakGives Member",
        currentUserEmail = user.email ?: "",
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
  currentUserId: String,
  currentUserName: String,
  currentUserEmail: String,
) {
  val viewModel: PakGivesViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        PakGivesViewModel(
          repository = PakGivesRepository(db),
          currentUserId = currentUserId,
          currentUserName = currentUserName,
          currentUserEmail = currentUserEmail,
        )
      }
    },
  )

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(statusMessage) {
    val msg = statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val navItems = remember {
    listOf(
      NavDestinationItem(ROUTE_NAMES, "Names", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_names"),
      NavDestinationItem(ROUTE_GIVEAWAYS, "Giveaways", Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard, "nav_giveaways"),
      NavDestinationItem(ROUTE_DONATE, "Donate", Icons.Filled.VolunteerActivism, Icons.Outlined.VolunteerActivism, "nav_donate"),
      NavDestinationItem(ROUTE_BUY_COINS, "Coins", Icons.Filled.MonetizationOn, Icons.Outlined.MonetizationOn, "nav_coins"),
      NavDestinationItem(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
    )
  }

  fun navigateTopLevel(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  val isTopLevelRoute = navItems.any { it.route == currentRoute }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavRail = maxWidth >= 600.dp

    Row(modifier = Modifier.fillMaxSize()) {
      if (useNavRail && isTopLevelRoute) {
        NavigationRail(
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.weight(1f))
          navItems.forEach { item ->
            val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
            NavigationRailItem(
              selected = selected,
              onClick = { navigateTopLevel(item.route) },
              icon = {
                Icon(
                  imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.label,
                )
              },
              label = { Text(item.label) },
              modifier = Modifier.testTag(item.testTag),
            )
          }
          Spacer(modifier = Modifier.weight(1f))
        }
      }

      Scaffold(
        modifier = Modifier.weight(1f),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
          if (isTopLevelRoute) {
            TopAppBar(
              title = {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              },
              actions = {
                Surface(
                  onClick = { navigateTopLevel(ROUTE_BUY_COINS) },
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(50),
                  modifier = Modifier
                    .padding(end = 12.dp)
                    .testTag("topbar_coin_pill"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.MonetizationOn,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "${profile?.coinBalance ?: 0L}",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
              ),
            )
          }
        },
        bottomBar = {
          if (!useNavRail && isTopLevelRoute) {
            NavigationBar(
              modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            ) {
              navItems.forEach { item ->
                val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopLevel(item.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                      contentDescription = item.label,
                    )
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
          contentAlignment = Alignment.TopCenter,
        ) {
          NavHost(
            navController = navController,
            startDestination = ROUTE_NAMES,
            modifier = Modifier
              .widthIn(max = 720.dp)
              .fillMaxSize(),
          ) {
            composable(ROUTE_NAMES) {
              NamesScreen(viewModel = viewModel)
            }
            composable(ROUTE_GIVEAWAYS) {
              GiveawaysScreen(
                viewModel = viewModel,
                onNavigateToBuyCoins = { navigateTopLevel(ROUTE_BUY_COINS) },
              )
            }
            composable(ROUTE_DONATE) {
              DonationsScreen(viewModel = viewModel)
            }
            composable(ROUTE_BUY_COINS) {
              BuyCoinsScreen(viewModel = viewModel)
            }
            composable(ROUTE_PROFILE) {
              ProfileScreen(
                viewModel = viewModel,
                onOpenTerms = { navController.navigate(ROUTE_TERMS) },
                onOpenAdminPanel = { navController.navigate(ROUTE_ADMIN) },
                onOpenBuyCoins = { navigateTopLevel(ROUTE_BUY_COINS) },
                onOpenGiveaways = { navigateTopLevel(ROUTE_GIVEAWAYS) },
                onSignOut = {
                  signOutUser(
                    credentialManager = credentialManager,
                    scope = coroutineScope,
                  )
                },
              )
            }
            composable(ROUTE_TERMS) {
              TermsPoliciesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
              )
            }
            composable(ROUTE_ADMIN) {
              AdminPanelScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
              )
            }
          }
        }
      }
    }
  }
}
