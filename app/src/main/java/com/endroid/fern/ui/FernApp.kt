


@Composable
private fun AdditionalContent() {
    val context = LocalContext.current
    val prefs = remember { com.endroid.fern.data.Prefs(context) }
    var overlayOn by remember { mutableStateOf(prefs.overlayEnabled) }
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var widthDp by remember { mutableFloatStateOf(prefs.overlayWidthDp.toFloat()) }
    var heightDp by remember { mutableFloatStateOf(prefs.overlayHeightDp.toFloat()) }
    var opacity by remember { mutableFloatStateOf(prefs.overlayOpacity) }
    var showCpu by remember { mutableStateOf(prefs.overlayShowCpu) }
    var showRam by remember { mutableStateOf(prefs.overlayShowRam) }
    var showGpu by remember { mutableStateOf(prefs.overlayShowGpu) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                canDraw = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    fun applySize() {
        prefs.overlayWidthDp = widthDp.toInt()
        prefs.overlayHeightDp = heightDp.toInt()
        prefs.overlayOpacity = opacity
        prefs.overlayShowCpu = showCpu
        prefs.overlayShowRam = showRam
        prefs.overlayShowGpu = showGpu
        if (overlayOn && canDraw) OverlayService.reload(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Floating island",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Draggable overlay for live CPU, RAM, and GPU. Needs Display over other apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show island", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (canDraw) "Permission granted" else "Needs overlay permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = overlayOn && canDraw,
                        onCheckedChange = { on ->
                            if (on && !canDraw) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + context.packageName)
                                    )
                                )
                                return@Switch
                            }
                            overlayOn = on
                            prefs.overlayEnabled = on
                            if (on) OverlayService.start(context) else OverlayService.stop(context)
                        }
                    )
                }
                if (!canDraw) {
                    FilledTonalButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant display over apps")
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Size and look", style = MaterialTheme.typography.titleSmall)
                Text("Width  " + widthDp.toInt() + " dp", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = widthDp,
                    onValueChange = { widthDp = it },
                    valueRange = 120f..420f,
                    onValueChangeFinished = { applySize() }
                )
                Text("Height  " + heightDp.toInt() + " dp", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = heightDp,
                    onValueChange = { heightDp = it },
                    valueRange = 80f..280f,
                    onValueChangeFinished = { applySize() }
                )
                Text("Opacity  " + (opacity * 100).toInt() + "%", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = 0.35f..1f,
                    onValueChangeFinished = { applySize() }
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Metrics", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CPU")
                    Switch(checked = showCpu, onCheckedChange = { showCpu = it; applySize() })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RAM")
                    Switch(checked = showRam, onCheckedChange = { showRam = it; applySize() })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GPU")
                    Switch(checked = showGpu, onCheckedChange = { showGpu = it; applySize() })
                }
                Text(
                    "GPU needs sysfs access; shows n/a when blocked. Drag the island to move it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
