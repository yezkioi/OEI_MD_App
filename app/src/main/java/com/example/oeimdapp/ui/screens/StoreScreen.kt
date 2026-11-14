package com.example.oeimdapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HorizontalSplit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.ui.zIndex
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.oeimdapp.data.model.ItemDto
import com.example.oeimdapp.data.model.ListDetailDto
import com.example.oeimdapp.ui.theme.ItemCodeTextStyle
import com.example.oeimdapp.ui.theme.OeiTheme
import com.example.oeimdapp.ui.theme.TabularBadgeTextStyle
import com.example.oeimdapp.ui.theme.TabularNumberTextStyle
import com.example.oeimdapp.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoreScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExportDialog: () -> Unit
) {
    val colors = OeiTheme.colors
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val storeId = viewModel.selectedStoreId
    val store = storeId?.let { viewModel.getStoreById(it) }

    if (store == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val storeLists = viewModel.getListsForStore(store)
    val currentList = storeLists.find { it.id == viewModel.selectedListId } ?: storeLists.firstOrNull()

    val pillListState = rememberLazyListState()
    LaunchedEffect(currentList?.id) {
        currentList?.let { list ->
            val index = storeLists.indexOfFirst { it.id == list.id }
            if (index >= 0) {
                pillListState.animateScrollToItem(index)
            }
        }
    }

    // State untuk berbagai modal dialog di layar toko
    var zoomItem by remember { mutableStateOf<ItemDto?>(null) }
    var keypadTargetItem by remember { mutableStateOf<ItemDto?>(null) }
    var isZeroCardKeypadOpen: Boolean by remember { mutableStateOf(false) }
    var isClearDialogOpen: Boolean by remember { mutableStateOf(false) }

    fun triggerUndoSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val dismissJob = launch {
                delay(5000L)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Urungkan",
                duration = SnackbarDuration.Indefinite
            )
            dismissJob.cancel()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoClear()
            }
        }
    }

    // Hitung jumlah list yang sudah memiliki minimal 1 item terisi untuk lencana tombol Export
    val filledListCount = storeLists.count { listDetail ->
        val listCounts = viewModel.currentStoreCounts[listDetail.id].orEmpty()
        listCounts.values.any { it > 0 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. TOP APP BAR
            // ==========================================
            Surface(
                color = colors.elevatedSurface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke daftar toko",
                                tint = colors.textPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = store.nama,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (store.area.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = store.area,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = "•",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                Text(
                                    text = "MD: ${viewModel.mdName}",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan",
                                tint = colors.textPrimary
                            )
                        }
                    }

                    // ==========================================
                    // 2. BARIS TOMBOL LIST GESER HORIZONTAL (LazyRow)
                    // ==========================================
                    if (storeLists.isNotEmpty()) {
                        LazyRow(
                            state = pillListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = storeLists,
                                key = { it.id }
                            ) { listDetail ->
                                val isSelected = listDetail.id == currentList?.id
                                val countsMap = viewModel.currentStoreCounts[listDetail.id].orEmpty()
                                val filledRegular = listDetail.items.count { (countsMap[it.kode] ?: 0) > 0 }
                                val totalRegular = listDetail.items.size

                                ListPillButton(
                                    listDetail = listDetail,
                                    filledCount = filledRegular,
                                    totalCount = totalRegular,
                                    isSelected = isSelected,
                                    onClick = { viewModel.selectList(listDetail.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Banner Notifikasi WH (jika ada pembaruan list)
            viewModel.listUpdatedBanner?.let { bannerText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.kurangCardBg)
                        .border(1.dp, colors.kurangBorder)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Info: $bannerText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.kurangBadgeText
                    )
                    TextButton(
                        onClick = { viewModel.dismissListUpdatedBanner() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Tutup",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.kurangBadgeText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // 4. GRID 3 KOLOM KARTU PRODUK (DENGAN DIRECTIONAL ANIMATED CONTENT & SWIPE)
            // ==========================================
            if (currentList != null) {
                val navigableSteps = remember(storeLists) {
                    val steps = mutableListOf<Pair<String, String>>()
                    for (list in storeLists) {
                        if (list.pisahOrientasi) {
                            steps.add(list.id to "Vertical")
                            steps.add(list.id to "Horizontal")
                        } else {
                            steps.add(list.id to "Vertical")
                        }
                    }
                    steps
                }

                val currentListId = currentList.id
                val currentOrient = viewModel.selectedOrientation
                val currentIndex = remember(currentListId, currentOrient, navigableSteps) {
                    navigableSteps.indexOfFirst { it.first == currentListId && it.second.equals(currentOrient, ignoreCase = true) }.coerceAtLeast(0)
                }
                var previousIndex by remember { mutableStateOf(currentIndex) }
                val isMovingForward = currentIndex >= previousIndex
                LaunchedEffect(currentIndex) {
                    previousIndex = currentIndex
                }

                var dragTotal by remember { mutableStateOf(0f) }
                val swipeModifier = Modifier
                    .fillMaxSize()
                    .pointerInput(navigableSteps, currentList.id, viewModel.selectedOrientation) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onHorizontalDrag = { _, change ->
                                dragTotal += change
                            },
                            onDragEnd = {
                                val threshold = 45f
                                if (dragTotal > threshold) {
                                    // Swipe Right (Left-to-Right gesture) -> Previous step
                                    if (currentIndex > 0) {
                                        val prev = navigableSteps[currentIndex - 1]
                                        viewModel.selectList(prev.first)
                                        val targetList = storeLists.find { it.id == prev.first }
                                        if (targetList != null && targetList.pisahOrientasi) {
                                            viewModel.selectOrientation(prev.second)
                                        }
                                    }
                                } else if (dragTotal < -threshold) {
                                    // Swipe Left (Right-to-Left gesture) -> Next step
                                    if (currentIndex >= 0 && currentIndex < navigableSteps.size - 1) {
                                        val next = navigableSteps[currentIndex + 1]
                                        viewModel.selectList(next.first)
                                        val targetList = storeLists.find { it.id == next.first }
                                        if (targetList != null && targetList.pisahOrientasi) {
                                            viewModel.selectOrientation(next.second)
                                        }
                                    }
                                }
                                dragTotal = 0f
                            }
                        )
                    }

                AnimatedContent(
                    targetState = currentList.id to viewModel.selectedOrientation,
                    transitionSpec = {
                        val forward = isMovingForward
                        val slideOffset = 300
                        if (forward) {
                            (slideInHorizontally(animationSpec = tween(220)) { slideOffset } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { -slideOffset } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(220)) { -slideOffset } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { slideOffset } + fadeOut(tween(180)))
                        }
                    },
                    label = "listOrientationTransition",
                    modifier = swipeModifier
                ) { (activeListId, activeOrientation) ->
                    val activeList = storeLists.find { it.id == activeListId } ?: currentList
                    val visibleItems = remember(activeList, activeOrientation) {
                        if (activeList.pisahOrientasi) {
                            activeList.items.filter {
                                it.orientasi.equals(activeOrientation, ignoreCase = true)
                            }
                        } else {
                            activeList.items
                        }
                    }

                    val zeroItemKey = remember(activeList.id, activeList.pisahOrientasi, activeOrientation) {
                        if (activeList.pisahOrientasi) {
                            if (activeOrientation.equals("Horizontal", ignoreCase = true)) "000-H"
                            else "000-V"
                        } else {
                            "000"
                        }
                    }
                    val zeroItemLabel = "${activeList.jenisAlias}-000"
                    val zeroItemCount = viewModel.getCount(activeList.id, zeroItemKey)

                    val gridTopPadding = if (activeList.pisahOrientasi) 64.dp else 8.dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 10.dp,
                            end = 10.dp,
                            top = gridTopPadding,
                            bottom = 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = visibleItems,
                            key = { "${activeList.id}_${it.kode}" },
                            contentType = { "product_stock_card" }
                        ) { item ->
                            val count = viewModel.getCount(activeList.id, item.kode)
                            ProductStockCard(
                                item = item,
                                pisahOrientasi = activeList.pisahOrientasi,
                                count = count,
                                onIncrement = { viewModel.updateItemCount(activeList.id, item.kode, count + 1) },
                                onDecrement = { viewModel.updateItemCount(activeList.id, item.kode, count - 1) },
                                onOpenKeypad = { keypadTargetItem = item },
                                onOpenZoom = { zoomItem = item }
                            )
                        }

                        // Kartu Khusus ALIAS-000 di posisi paling akhir grid
                        item(
                            key = "${activeList.id}_$zeroItemKey",
                            contentType = "extra_zero_card"
                        ) {
                            ExtraZeroStockCard(
                                label = zeroItemLabel,
                                count = zeroItemCount,
                                onIncrement = { viewModel.updateItemCount(activeList.id, zeroItemKey, zeroItemCount + 1) },
                                onDecrement = { viewModel.updateItemCount(activeList.id, zeroItemKey, zeroItemCount - 1) },
                                onOpenKeypad = { isZeroCardKeypadOpen = true }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. SNACKBAR URUNGKAN (DI ATAS FLOATING DOCK DENGAN COUNTDOWN LINE)
        // ==========================================
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 76.dp, start = 12.dp, end = 12.dp)
        ) { data ->
            val progress = remember { Animatable(1f) }
            LaunchedEffect(Unit) {
                progress.animateTo(0f, animationSpec = tween(5000, easing = LinearEasing))
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.borderStrong),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        data.visuals.actionLabel?.let { action ->
                            TextButton(onClick = { data.performAction() }) {
                                Text(
                                    text = action,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryAction
                                )
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = colors.primaryAction,
                        trackColor = colors.subtleSurface
                    )
                }
            }
        }

        // ==========================================
        // 6. WARM CRAFT FLOATING PILL DOCK (CLEAR & EXPORT XLSX)
        // ==========================================
        if (currentList != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = CircleShape,
                color = colors.elevatedSurface,
                border = BorderStroke(1.dp, colors.borderStrong),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kiri: Tombol Kapsul Clear
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.subtleSurfaceAlt)
                            .border(1.dp, colors.borderStrong, CircleShape)
                            .clickable { isClearDialogOpen = true }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = colors.habisBorder,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Clear",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }

                    // Kanan: Tombol Kapsul Utama Export XLSX + Lencana Progres List
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.primaryAction)
                            .clickable { onOpenExportDialog() }
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = colors.onPrimaryAction,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Export XLSX",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimaryAction
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.onPrimaryAction.copy(alpha = 0.18f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$filledListCount/${storeLists.size}",
                                    style = TabularBadgeTextStyle,
                                    color = colors.onPrimaryAction
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // FLOATING ORIENTATION BAR (ROUNDED SEPERTI CLEAR/EXPORT DENGAN GIMMICK ICON)
        // ==========================================
        AnimatedVisibility(
            visible = currentList != null && currentList.pisahOrientasi,
            enter = fadeIn(tween(250)) + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut(tween(200)) + shrinkVertically(shrinkTowards = Alignment.Top),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 125.dp, start = 16.dp, end = 16.dp)
                .zIndex(10f)
        ) {
            if (currentList != null && currentList.pisahOrientasi) {
                val orientations = listOf(
                    "Vertical" to Icons.Default.VerticalSplit,
                    "Horizontal" to Icons.Default.HorizontalSplit
                )
                val isHorizontalSelected = viewModel.selectedOrientation.equals("Horizontal", ignoreCase = true)
                val targetBias by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isHorizontalSelected) 1f else -1f,
                    animationSpec = tween(250),
                    label = "floatingOrientationAnimation"
                )

                Surface(
                    shape = CircleShape,
                    color = colors.elevatedSurface,
                    border = BorderStroke(1.dp, colors.borderStrong),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .align(androidx.compose.ui.BiasAlignment(horizontalBias = targetBias, verticalBias = 0f))
                                .clip(CircleShape)
                                .background(colors.primaryAction)
                                .height(40.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            orientations.forEach { (orientation, iconVec) ->
                                val isTabSelected = viewModel.selectedOrientation.equals(orientation, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .clickable { viewModel.selectOrientation(orientation) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = iconVec,
                                            contentDescription = null,
                                            tint = if (isTabSelected) colors.onPrimaryAction else colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = orientation,
                                            fontSize = 13.sp,
                                            fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isTabSelected) colors.onPrimaryAction else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    // ==========================================
    // RENDER MODAL DIALOGS
    // ==========================================
    val activeList = currentList
    if (activeList != null) {
        // 1. Zoom Lightbox Dialog
        val zItem = zoomItem
        if (zItem != null) {
            val liveCount = viewModel.getCount(activeList.id, zItem.kode)
            ZoomLightboxDialog(
                item = zItem,
                showOrientation = activeList.pisahOrientasi,
                currentCount = liveCount,
                onUpdateCount = { newVal: Int -> viewModel.updateItemCount(activeList.id, zItem.kode, newVal) },
                onDismiss = { zoomItem = null }
            )
        }

        // 2. Modal Numeric Keypad untuk Item Reguler
        val kItem = keypadTargetItem
        if (kItem != null) {
            val liveCount = viewModel.getCount(activeList.id, kItem.kode)
            NumericKeypadDialog(
                itemLabel = kItem.label,
                target = kItem.target,
                initialValue = liveCount,
                onDismiss = { keypadTargetItem = null },
                onConfirm = { newVal: Int ->
                    viewModel.updateItemCount(activeList.id, kItem.kode, newVal)
                    keypadTargetItem = null
                }
            )
        }

        // 3. Modal Numeric Keypad untuk Kartu ALIAS-000
        if (isZeroCardKeypadOpen) {
            val zeroKey = if (activeList.pisahOrientasi) {
                if (viewModel.selectedOrientation.equals("Horizontal", ignoreCase = true)) "000-H"
                else "000-V"
            } else {
                "000"
            }
            val liveZeroCount = viewModel.getCount(activeList.id, zeroKey)
            NumericKeypadDialog(
                itemLabel = "${activeList.jenisAlias}-000",
                target = null,
                initialValue = liveZeroCount,
                onDismiss = { isZeroCardKeypadOpen = false },
                onConfirm = { newVal: Int ->
                    viewModel.updateItemCount(activeList.id, zeroKey, newVal)
                    isZeroCardKeypadOpen = false
                }
            )
        }

        // 4. Dialog Konfirmasi Clear
        if (isClearDialogOpen) {
            ClearConfirmDialog(
                currentListName = activeList.nama,
                storeName = store.nama,
                onClearCurrentList = {
                    isClearDialogOpen = false
                    viewModel.clearCurrentList()
                    triggerUndoSnackbar("List ${activeList.nama} direset ke 0")
                },
                onClearAllStoreLists = {
                    isClearDialogOpen = false
                    viewModel.clearAllListsInStore()
                    triggerUndoSnackbar("Semua list ${store.nama} direset ke 0")
                },
                onDismiss = { isClearDialogOpen = false }
            )
        }
    }
}
}

// ==========================================
// KOMPONEN PILL TOMBOL LIST DI ATAS
// ==========================================
@Composable
private fun ListPillButton(
    listDetail: ListDetailDto,
    filledCount: Int,
    totalCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = OeiTheme.colors
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) colors.primaryAction else colors.subtleSurfaceAlt,
        animationSpec = tween(300),
        label = "pillBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) colors.primaryAction else colors.borderStrong,
        animationSpec = tween(300),
        label = "pillBorder"
    )
    val contentColor = if (isSelected) colors.onPrimaryAction else colors.textPrimary
    val badgeBg = if (isSelected) colors.onPrimaryAction.copy(alpha = 0.2f) else colors.elevatedSurface
    val badgeTextColor = if (isSelected) colors.onPrimaryAction else colors.textSecondary

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(animatedBg)
            .border(
                width = 1.dp,
                color = animatedBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = listDetail.nama,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeBg)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = "$filledCount/$totalCount",
                style = TabularBadgeTextStyle,
                color = badgeTextColor
            )
        }
    }
}

// ==========================================
// KARTU PRODUK REGULER (RATIO 1:1 SERAGAM)
// ==========================================
@Composable
private fun ProductStockCard(
    item: ItemDto,
    pisahOrientasi: Boolean,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onOpenKeypad: () -> Unit,
    onOpenZoom: () -> Unit
) {
    val colors = OeiTheme.colors
    var useFallbackUrl: Boolean by remember(item.fileId) { mutableStateOf(false) }

    val borderColor: Color
    val photoBgColor: Color
    val cardBgColor: Color
    val badgeBgColor: Color
    val badgeTextColor: Color
    val statusLabel: String
    val stepperBtnBg: Color
    val stepperBtnText: Color
    val counterBg: Color
    val counterText: Color

    when {
        count <= 0 -> {
            borderColor = colors.habisBorder
            photoBgColor = colors.habisPhotoBg
            cardBgColor = colors.habisCardBg
            badgeBgColor = colors.habisBadgeBg
            badgeTextColor = colors.habisBadgeText
            statusLabel = "Habis"
            stepperBtnBg = colors.habisStepperBtnBg
            stepperBtnText = colors.habisStepperBtnText
            counterBg = colors.habisCounterBg
            counterText = colors.habisCounterText
        }
        count < item.target -> {
            borderColor = colors.kurangBorder
            photoBgColor = colors.kurangPhotoBg
            cardBgColor = colors.kurangCardBg
            badgeBgColor = colors.kurangBadgeBg
            badgeTextColor = colors.kurangBadgeText
            statusLabel = "Kurang ${item.target - count}"
            stepperBtnBg = colors.kurangStepperBtnBg
            stepperBtnText = colors.kurangStepperBtnText
            counterBg = colors.kurangCounterBg
            counterText = colors.kurangCounterText
        }
        else -> {
            borderColor = colors.penuhBorder
            photoBgColor = colors.penuhPhotoBg
            cardBgColor = colors.penuhCardBg
            badgeBgColor = colors.penuhBadgeBg
            badgeTextColor = colors.penuhBadgeText
            statusLabel = "Penuh ✓"
            stepperBtnBg = colors.penuhStepperBtnBg
            stepperBtnText = colors.penuhStepperBtnText
            counterBg = colors.penuhCounterBg
            counterText = colors.penuhCounterText
        }
    }

    // Poin 1: Menyatukan seluruh card produk ke rasio 1:1 yang pas dan seragam
    val imageAspectRatio = 1f

    val imageUrl = if (!useFallbackUrl) {
        "https://drive.google.com/thumbnail?id=${item.fileId}&sz=w400"
    } else {
        "https://drive.google.com/uc?export=view&id=${item.fileId}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cardBgColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspectRatio)
                    .background(photoBgColor)
                    .clickable(onClick = onOpenZoom),
                contentAlignment = Alignment.Center
            ) {
                if (item.fileId.isBlank()) {
                    Column(
                        modifier = Modifier.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = ItemCodeTextStyle.copy(fontSize = 10.sp),
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        onError = {
                            if (!useFallbackUrl) {
                                useFallbackUrl = true
                            }
                        }
                    )
                }

                if (item.bestSeller) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.bestBadgeBg)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "★ Best",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.bestBadgeText
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = ItemCodeTextStyle.copy(fontSize = 11.sp),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "T:${item.target}",
                    style = TabularBadgeTextStyle.copy(fontSize = 10.sp),
                    color = colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBgColor)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(stepperBtnBg)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = stepperBtnText)
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .background(counterBg)
                        .clickable(onClick = onOpenKeypad),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = count.toString(), style = TabularNumberTextStyle.copy(fontSize = 14.sp), color = counterText)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(stepperBtnBg)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = stepperBtnText)
                }
            }
        }
    }
}

// ==========================================
// KARTU KHUSUS ALIAS-000 (GARIS PUTUS-PUTUS KELILING & BG Kontras)
// ==========================================
@Composable
private fun ExtraZeroStockCard(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onOpenKeypad: () -> Unit
) {
    val colors = OeiTheme.colors
    val dashedBorderColor = colors.zeroCardBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.elevatedSurface)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                // Poin 6: Menggambar path dashed full di keliling card dengan inset agar sudutnya presisi
                drawRoundRect(
                    color = dashedBorderColor,
                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Poin 5: zeroPlaceholderBg disesuaikan agar kontras pada tema terang
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(colors.zeroPlaceholderBg)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Di luar list",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = ItemCodeTextStyle.copy(fontSize = 11.sp),
                    color = colors.textPrimary,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.subtleSurfaceAlt)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tanpa Target",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(colors.subtleSurfaceAlt)
                        .clickable(onClick = onDecrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "−", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .background(colors.elevatedSurface)
                        .clickable(onClick = onOpenKeypad),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = count.toString(), style = TabularNumberTextStyle.copy(fontSize = 14.sp), color = colors.textPrimary)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(colors.subtleSurfaceAlt)
                        .clickable(onClick = onIncrement),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
        }
    }
}