import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.LineBorder
import javax.imageio.ImageIO
import java.io.File
import java.awt.dnd.*
import java.awt.datatransfer.DataFlavor
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
class TileMapEditorApp : JFrame("Setshape Editor") {
    private var tileMap = IntArray(64 * 64) { 0 }
    private var selectedBrush = 22
    data class TileType(val id: Int, val color: Color, val description: String)
    private val tileTypes = listOf(
        TileType(0, Color(0, 0, 0), ""),
        TileType(1, Color(0, 0, 0), ""),
        TileType(2, Color(194, 35, 35), "Hurt Underground"),
        TileType(3, Color(156, 107, 66), "Chair"),
        TileType(4, Color(189, 189, 255), "Bed Upper"),
        TileType(5, Color(223, 223, 255), "Bed Lower"),
        TileType(6, Color(41, 123, 57), "Swamp"),
        TileType(7, Color(99, 0, 0), "Lava Swamp"),
        TileType(8, Color(90, 132, 198), "Shallow Water"),
        TileType(9, Color(0, 0, 0), ""),
        TileType(10, Color(0, 0, 0), ""),
        TileType(11, Color(57, 99, 165), "Water"),
        TileType(12, Color(255, 0, 0), "Lava"),
        TileType(13, Color(0, 0, 0), ""),
        TileType(14, Color(0, 0, 0), ""),
        TileType(15, Color(0, 0, 0), ""),
        TileType(16, Color(0, 0, 0), ""),
        TileType(17, Color(0, 0, 0), ""),
        TileType(18, Color(0, 0, 0), ""),
        TileType(19, Color(0, 0, 0), ""),
        TileType(20, Color(99, 82, 49), "Throw-through"),
        TileType(21, Color(123, 189, 148), "Jumping"),
        TileType(22, Color(128, 0, 128), "Blocking")
    )
    private val importButton = getPlaceholderButton("Import")
    private val generateButton = getPlaceholderButton("Generate")
    private val clearButton = getPlaceholderButton("Clear")
    private val loadImageButton = getPlaceholderButton("Load Image")
    private val cutButton = getPlaceholderButton("Cut")
    private val pickerPanel = JPanel()
    private val tileMapPanel = TileMapPanel()
    private var cutMode = false
    private var currentImageFile: File? = null
    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        setSize(400, 400)
        setLocationRelativeTo(null)
        contentPane.background = Color(30, 30, 30)
        try {
            val iconURL = javaClass.classLoader.getResource("setshape2.png")
            if (iconURL != null) {
                iconImage = ImageIO.read(iconURL)
            } else {
                println("Could not find setshape2.png in resources.")
            }
        } catch (e: Exception) {
            println("Could not load application icon: ${e.message}")
        }
        setupUI()
        setupEventListeners()
        selectTileType(22)
        tileMapPanel.updateTileMap()
    }
    fun getPlaceholderButton(text : String) : JButton {      
            return JButton(text).apply{
                background = Color(70, 70, 70)
                foreground = Color(220, 220, 220)
                border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
                isFocusPainted = false
                isContentAreaFilled = false
                isOpaque = true
                font = Font("Arial", Font.BOLD, 12)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseEntered(e: MouseEvent) {
                        background = Color(80, 80, 80)

                    }
                    override fun mouseExited(e: MouseEvent) {
                        background = Color(70, 70, 70)
                    }
                })
            }
    }
    private fun setupUI() {
        layout = BorderLayout()
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = Color(30, 30, 30)
            add(importButton)
            add(generateButton)
            add(clearButton)
            add(loadImageButton)
            add(cutButton)
        }
        pickerPanel.apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Color(40, 40, 40)
            preferredSize = Dimension(40, 0)
            tileTypes.forEach { tileType ->
                if (tileType.description.isNotEmpty()) {
                    val swatch = TileTypeSwatch(tileType)
                    swatch.addMouseListener(object : MouseAdapter() {
                        override fun mousePressed(e: MouseEvent) {
                            selectTileType(tileType.id)
                        }
                    })
                    add(swatch)
                }
            }
        }
        add(topPanel, BorderLayout.NORTH)
        add(pickerPanel, BorderLayout.WEST)
        add(tileMapPanel, BorderLayout.CENTER)
    }
    private fun setupEventListeners() {
        importButton.addActionListener {
            showImportDialog()
        }
        generateButton.addActionListener {
            generateCode()
        }
        clearButton.addActionListener {
            tileMap = IntArray(64 * 64) { 0 }
            tileMapPanel.updateTileMap()
        }
        loadImageButton.addActionListener {
            loadImage()
        }
        cutButton.addActionListener {
            cutMode = !cutMode
            cutButton.text = if (cutMode) "Cancel" else "Cut"
            pickerPanel.isVisible = !cutMode
            title = if (cutMode) "Setimgpart Editor" else "Setshape Editor"
            tileMapPanel.cutMode = cutMode
            tileMapPanel.repaint()
        }
    }
    private fun loadImage() {
        val dialog = FileDialog(this, "Select Image", FileDialog.LOAD)
        dialog.setFilenameFilter { dir, name -> 
            name.lowercase().endsWith(".png") || 
            name.lowercase().endsWith(".jpg") || 
            name.lowercase().endsWith(".jpeg") || 
            name.lowercase().endsWith(".gif") ||
            name.lowercase().endsWith(".mng")
        }
        dialog.isVisible = true
        val filename = dialog.file
        val directory = dialog.directory
        if (filename != null && directory != null) {
            try {
                val file = File(directory, filename)
                val image = ImageIO.read(file)
                currentImageFile = file
                tileMapPanel.setBackgroundImage(image)
                val imageWidth = image.getWidth(null)
                val imageHeight = image.getHeight(null)
                if (imageWidth > tileMapPanel.width || imageHeight > tileMapPanel.height) {
                    val newWidth = max(imageWidth + 100, 500)
                    val newHeight = max(imageHeight + 150, 500)
                    setSize(newWidth, newHeight)
                    setLocationRelativeTo(null)
                }
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(this, "Failed to load image: ${e.message}", 
                    "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
    private fun selectTileType(id: Int) {
        selectedBrush = id
        for (component in pickerPanel.components) {
            if (component is TileTypeSwatch) {
                component.setSelected(component.tileType.id == id)
            }
        }
    }
    private fun generateCode() {
        var maxW = 0
        var maxH = 0
        for (i in tileMap.indices) {
            val x = i % 64
            val y = i / 64
            val t = tileMap[i]
            if (t > 0) {
                if (x > maxW) maxW = x
                if (y > maxH) maxH = y
            }
        }
        maxW++
        maxH++
        val tiles = mutableListOf<Int>()
        for (i in 0 until maxW * maxH) {
            val x = i % maxW
            val y = (i / maxW) * 64
            tiles.add(tileMap[x + y])
        }
        showOutputWindow(tiles, maxW, maxH)
    }
    private fun showOutputWindow(tiles: List<Int>, w: Int, h: Int) {
        val outputFrame = JFrame("SetShape2 Output")
        outputFrame.setSize(400, 400)
        outputFrame.layout = BorderLayout()
        outputFrame.contentPane.background = Color(30, 30, 30)
        val outputText = JTextArea()
        outputText.isEditable = false
        outputText.background = Color(40, 40, 40)
        outputText.foreground = Color.WHITE
        outputText.font = Font("Courier New", Font.PLAIN, 12)
        val scrollPane = JScrollPane(outputText)
        scrollPane.border = null
        val bottomPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = Color(30, 30, 30)
            val indentSlider = JSlider(0, 20, 1)
            indentSlider.background = Color(30, 30, 30)
            indentSlider.foreground = Color.WHITE
            val gs1Checkbox = JCheckBox("GS1")
            gs1Checkbox.background = Color(30, 30, 30)
            gs1Checkbox.foreground = Color.WHITE
            val closeButton = JButton("Close")
            closeButton.background = Color(70, 70, 70)
            closeButton.foreground = Color(220, 220, 220)
            closeButton.border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
            closeButton.isFocusPainted = false
            closeButton.isContentAreaFilled = false
            closeButton.isOpaque = true
            closeButton.font = Font("Arial", Font.BOLD, 12)
            closeButton.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    closeButton.background = Color(80, 80, 80)
                }
                override fun mouseExited(e: MouseEvent) {
                    closeButton.background = Color(70, 70, 70)
                }
            })
            closeButton.addActionListener {
                outputFrame.dispose()
            }
            add(JLabel("Indent:").apply { foreground = Color.WHITE })
            add(indentSlider)
            add(gs1Checkbox)
            add(closeButton)
            indentSlider.addChangeListener {
                updateOutput(outputText, tiles, w, h, indentSlider.value, gs1Checkbox.isSelected)
            }
            gs1Checkbox.addActionListener {
                updateOutput(outputText, tiles, w, h, indentSlider.value, gs1Checkbox.isSelected)
            }
        }
        outputFrame.add(scrollPane, BorderLayout.CENTER)
        outputFrame.add(bottomPanel, BorderLayout.SOUTH)
        updateOutput(outputText, tiles, w, h, 1, false)
        outputFrame.setLocationRelativeTo(this)
        outputFrame.iconImage = this.iconImage
        outputFrame.isVisible = true
    }
    private fun updateOutput(textArea: JTextArea, tiles: List<Int>, w: Int, h: Int, indent: Int, gs1: Boolean) {
        val indentStr = " ".repeat(indent * 2)
        val output = StringBuilder()
        if (gs1) {
            output.append("${indentStr}setshape2 $w,$h,{\n")
        } else {
            output.append("${indentStr}setshape2($w,$h,{\n")
        }
        for (i in tiles.indices) {
            if (i % w == 0) {
                output.append("$indentStr  ")
            }
            output.append("${tiles[i]}${if (tiles[i].toString().length == 1) " " else ""},")
            if ((i + 1) % w == 0) {
                output.append("\n")
            }
        }
        if (gs1) {
            output.append("$indentStr};")
        } else {
            output.append("$indentStr});")
        }
        textArea.text = output.toString()
    }
    private fun showImportDialog() {
        val importFrame = JFrame("Import Setshape2")
        importFrame.setSize(600, 400)
        importFrame.layout = BorderLayout()
        importFrame.contentPane.background = Color(30, 30, 30)
        val importText = JTextArea()
        importText.background = Color(40, 40, 40)
        importText.foreground = Color.WHITE
        importText.font = Font("Courier New", Font.PLAIN, 12)
        importText.text = ""
        val scrollPane = JScrollPane(importText)
        scrollPane.border = null
        val hintLabel = JLabel("Paste setshape2 code here (supports both GS1 and GS2 syntax)")
        hintLabel.foreground = Color(136, 136, 136)
        hintLabel.font = Font("Arial", Font.PLAIN, 11)
        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            background = Color(30, 30, 30)
            val importConfirmButton = JButton("Import")
            importConfirmButton.background = Color(70, 70, 70)
            importConfirmButton.foreground = Color(220, 220, 220)
            importConfirmButton.border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
            importConfirmButton.isFocusPainted = false
            importConfirmButton.isContentAreaFilled = false
            importConfirmButton.isOpaque = true
            importConfirmButton.font = Font("Arial", Font.BOLD, 12)
            importConfirmButton.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    importConfirmButton.background = Color(80, 80, 80)
                }
                override fun mouseExited(e: MouseEvent) {
                    importConfirmButton.background = Color(70, 70, 70)
                }
            })
            val cancelButton = JButton("Cancel")
            cancelButton.background = Color(70, 70, 70)
            cancelButton.foreground = Color(220, 220, 220)
            cancelButton.border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
            cancelButton.isFocusPainted = false
            cancelButton.isContentAreaFilled = false
            cancelButton.isOpaque = true
            cancelButton.font = Font("Arial", Font.BOLD, 12)
            cancelButton.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    cancelButton.background = Color(80, 80, 80)
                }
                override fun mouseExited(e: MouseEvent) {
                    cancelButton.background = Color(70, 70, 70)
                }
            })
            importConfirmButton.addActionListener {
                importSetshape(importText.text.trim())
                importFrame.dispose()
            }
            cancelButton.addActionListener {
                importFrame.dispose()
            }
            add(importConfirmButton)
            add(cancelButton)
        }
        val centerPanel = JPanel(BorderLayout())
        centerPanel.background = Color(30, 30, 30)
        centerPanel.add(scrollPane, BorderLayout.CENTER)
        centerPanel.add(hintLabel.apply {
            border = BorderFactory.createEmptyBorder(10, 10, 0, 10)
        }, BorderLayout.SOUTH)
        importFrame.add(centerPanel, BorderLayout.CENTER)
        importFrame.add(bottomPanel, BorderLayout.SOUTH)
        importFrame.setLocationRelativeTo(this)
        importFrame.iconImage = this.iconImage
        importFrame.isVisible = true
        importText.requestFocus()
    }
    private fun importSetshape(code: String) {
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please paste setshape2 code", "Error", JOptionPane.ERROR_MESSAGE)
            return
        }
        try {
            val parsed = parseSetshape2(code)
            if (parsed != null) {
                tileMap = IntArray(64 * 64) { 0 }
                for (i in parsed.tiles.indices) {
                    if (i < tileMap.size) {
                        val x = i % parsed.width
                        val y = i / parsed.width
                        if (x < 64 && y < 64) {
                            val index = x + y * 64
                            tileMap[index] = parsed.tiles[i]
                        }
                    }
                }
                tileMapPanel.resetView()
                tileMapPanel.updateTileMap()
            } else {
                JOptionPane.showMessageDialog(this, "Failed to parse setshape2 code. Please check the format.", "Error", JOptionPane.ERROR_MESSAGE)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(this, "Error importing: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
        }
    }
    private fun parseSetshape2(code: String): ParsedSetshape2? {
        var cleanedCode = code.replace(Regex("//.*"), "").replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        val gs1Match = Regex("setshape2\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*\\{([\\s\\S]*?)\\}\\s*;").find(cleanedCode)
        val gs2Match = Regex("setshape2\\s*\\(\\s*[\"']?(\\d+)[\"']?\\s*,\\s*(\\d+)\\s*,\\s*\\{([\\s\\S]*?)\\}\\s*\\)\\s*;").find(cleanedCode)
        val match = gs1Match ?: gs2Match
        if (match == null) return null
        val width = match.groupValues[1].toInt()
        val height = match.groupValues[2].toInt()
        val dataStr = match.groupValues[3]
        val numbers = Regex("\\d+").findAll(dataStr).map { it.value.toInt() }.toList()
        if (numbers.isEmpty()) return null
        if (numbers.size != width * height) return null
        return ParsedSetshape2(width, height, numbers)
    }
    private data class ParsedSetshape2(val width: Int, val height: Int, val tiles: List<Int>)
    private fun showSetimgpartOutput() {
        val selection = tileMapPanel.currentSelection ?: return
        val outputFrame = JFrame("Setimgpart Output")
        outputFrame.setSize(400, 200)
        outputFrame.layout = BorderLayout()
        outputFrame.contentPane.background = Color(30, 30, 30)
        val outputText = JTextArea()
        outputText.isEditable = false
        outputText.background = Color(40, 40, 40)
        outputText.foreground = Color.WHITE
        outputText.font = Font("Courier New", Font.PLAIN, 12)
        val imageName = currentImageFile?.name ?: "imagename.png"
        outputText.text = "setimgpart(\"$imageName\", ${selection.x}, ${selection.y}, ${selection.width}, ${selection.height});"
        val scrollPane = JScrollPane(outputText)
        scrollPane.border = null
        val closeButton = JButton("Close")
        closeButton.background = Color(70, 70, 70)
        closeButton.foreground = Color(220, 220, 220)
        closeButton.border = BorderFactory.createEmptyBorder(8, 20, 8, 20)
        closeButton.isFocusPainted = false
        closeButton.isContentAreaFilled = false
        closeButton.isOpaque = true
        closeButton.font = Font("Arial", Font.BOLD, 12)
        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                closeButton.background = Color(80, 80, 80)
            }
            override fun mouseExited(e: MouseEvent) {
                closeButton.background = Color(70, 70, 70)
            }
        })
        closeButton.addActionListener {
            outputFrame.dispose()
        }
        val bottomPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        bottomPanel.background = Color(30, 30, 30)
        bottomPanel.add(closeButton)
        outputFrame.add(scrollPane, BorderLayout.CENTER)
        outputFrame.add(bottomPanel, BorderLayout.SOUTH)
        outputFrame.setLocationRelativeTo(this)
        outputFrame.iconImage = this.iconImage
        outputFrame.isVisible = true
    }
    inner class TileMapPanel : JPanel() {
        private val tileSize = 16
        private var backgroundImage: Image? = null
        private var offsetX = 0
        private var offsetY = 0
        private var lastDragX = 0
        private var lastDragY = 0
        private var isDragging = false
        private var zoomLevel = 1.0
        var cutMode = false
        private var selectionStart: Point? = null
        private var selectionEnd: Point? = null
        var currentSelection: Rectangle? = null
        init {
            background = Color(50, 50, 50)
            preferredSize = Dimension(64 * tileSize, 64 * tileSize)
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON2) {
                        isDragging = true
                        lastDragX = e.x
                        lastDragY = e.y
                    } else if (cutMode && (e.button == MouseEvent.BUTTON1 || e.button == MouseEvent.BUTTON3)) {
                        val x = (e.x - offsetX) / zoomLevel
                        val y = (e.y - offsetY) / zoomLevel
                        if (e.isShiftDown || e.button == MouseEvent.BUTTON3) {
                            selectionStart = Point(((x / tileSize).toInt() * tileSize).toInt(), ((y / tileSize).toInt() * tileSize).toInt())
                        } else {
                            selectionStart = Point(x.toInt(), y.toInt())
                        }
                        selectionEnd = selectionStart
                        currentSelection = null
                    } else {
                        handleMouseClick(e)
                    }
                }
                override fun mouseReleased(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON2) {
                        isDragging = false
                    } else if (cutMode && (e.button == MouseEvent.BUTTON1 || e.button == MouseEvent.BUTTON3)) {
                        val start = selectionStart
                        val end = selectionEnd
                        if (start != null && end != null) {
                            val x = min(start.x, end.x)
                            val y = min(start.y, end.y)
                            val width = abs(end.x - start.x)
                            val height = abs(end.y - start.y)
                            if (width > 0 && height > 0) {
                                currentSelection = Rectangle(x, y, width, height)
                                this@TileMapEditorApp.showSetimgpartOutput()
                            }
                        }
                        selectionStart = null
                        selectionEnd = null
                    }
                }
            })
            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    if (isDragging && SwingUtilities.isMiddleMouseButton(e)) {
                        val dx = e.x - lastDragX
                        val dy = e.y - lastDragY
                        offsetX += dx
                        offsetY += dy
                        lastDragX = e.x
                        lastDragY = e.y
                        repaint()
                    } else if (cutMode && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e))) {
                        val x = (e.x - offsetX) / zoomLevel
                        val y = (e.y - offsetY) / zoomLevel
                        if (e.isShiftDown || SwingUtilities.isRightMouseButton(e)) {
                            selectionEnd = Point(((x / tileSize).toInt() * tileSize).toInt(), ((y / tileSize).toInt() * tileSize).toInt())
                        } else {
                            selectionEnd = Point(x.toInt(), y.toInt())
                        }
                        repaint()
                    } else {
                        handleMouseClick(e)
                    }
                }
            })
            DropTarget(this, object : DropTargetListener {
                override fun dragEnter(e: DropTargetDragEvent) { if (e.transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) e.acceptDrag(DnDConstants.ACTION_COPY) }
                override fun dragOver(e: DropTargetDragEvent) { if (e.transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) e.acceptDrag(DnDConstants.ACTION_COPY) else e.rejectDrag() }
                override fun dropActionChanged(e: DropTargetDragEvent) {}
                override fun dragExit(e: DropTargetEvent) {}
                override fun drop(e: DropTargetDropEvent) {
                    try {
                        e.acceptDrop(DnDConstants.ACTION_COPY)
                        val files = e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        val file = files.firstOrNull() as? File ?: return
                        if (file.extension.lowercase() in listOf("png", "jpg", "jpeg", "gif", "mng", "bmp")) {
                            val image = ImageIO.read(file) ?: return
                            currentImageFile = file
                            setBackgroundImage(image)
                            val newWidth = max(image.getWidth(null) + 100, 500)
                            val newHeight = max(image.getHeight(null) + 150, 500)
                            this@TileMapEditorApp.setSize(newWidth, newHeight)
                            this@TileMapEditorApp.setLocationRelativeTo(null)
                        }
                    } catch (_: Exception) {}
                }
            })
            addMouseWheelListener(object : MouseWheelListener {
                override fun mouseWheelMoved(e: MouseWheelEvent) {
                    val oldZoom = zoomLevel
                    val notches = e.wheelRotation
                    if (notches < 0) {
                        zoomLevel = min(zoomLevel * 1.1, 5.0)
                    } else {
                        zoomLevel = max(zoomLevel / 1.1, 0.25)
                    }
                    val mouseX = e.x
                    val mouseY = e.y
                    val zoomRatio = zoomLevel / oldZoom
                    offsetX = (mouseX - (mouseX - offsetX) * zoomRatio).toInt()
                    offsetY = (mouseY - (mouseY - offsetY) * zoomRatio).toInt()
                    repaint()
                }
            })
        }
        fun setBackgroundImage(image: Image) {
            backgroundImage = image
            repaint()
        }
        private fun handleMouseClick(e: MouseEvent) {
            val adjustedX = (e.x - offsetX) / zoomLevel
            val adjustedY = (e.y - offsetY) / zoomLevel
            val x = (adjustedX / tileSize).toInt()
            val y = (adjustedY / tileSize).toInt()
            if (x in 0..63 && y in 0..63) {
                val index = x + y * 64
                if (SwingUtilities.isLeftMouseButton(e)) {
                    tileMap[index] = selectedBrush
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    tileMap[index] = 0
                }
                updateTileMap()
            }
        }
        fun updateTileMap() {
            repaint()
        }
        fun resetView() {
            offsetX = 0
            offsetY = 0
            zoomLevel = 1.0
            repaint()
        }
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.translate(offsetX, offsetY)
            g2d.scale(zoomLevel, zoomLevel)
            val bgImage = backgroundImage
            if (bgImage != null) {
                g2d.color = Color(0, 0, 0, 100)
                g2d.fillRect(0, 0, bgImage.getWidth(null), bgImage.getHeight(null))
                g2d.drawImage(bgImage, 0, 0, bgImage.getWidth(null), bgImage.getHeight(null), this)
            }
            for (x in 0..63) {
                for (y in 0..63) {
                    g2d.color = Color(60, 60, 60)
                    g2d.drawRect(x * tileSize, y * tileSize, tileSize, tileSize)
                }
            }
            for (i in tileMap.indices) {
                val tileId = tileMap[i]
                if (tileId > 0 && tileId < tileTypes.size) {
                    val x = i % 64
                    val y = i / 64
                    val tileType = tileTypes[tileId]
                    g2d.color = tileType.color
                    g2d.fillRect(x * tileSize, y * tileSize, tileSize, tileSize)
                    g2d.color = tileType.color.darker()
                    g2d.drawRect(x * tileSize, y * tileSize, tileSize, tileSize)
                }
            }
            if (cutMode) {
                if (selectionStart != null && selectionEnd != null) {
                    val start = selectionStart!!
                    val end = selectionEnd!!
                    val x = min(start.x, end.x)
                    val y = min(start.y, end.y)
                    val width = abs(end.x - start.x)
                    val height = abs(end.y - start.y)
                    g2d.color = Color(255, 255, 0, 128)
                    g2d.fillRect(x, y, width, height)
                    g2d.color = Color(255, 255, 0)
                    g2d.drawRect(x, y, width, height)
                    g2d.color = Color.WHITE
                    g2d.font = Font("Arial", Font.BOLD, (12 / zoomLevel).toInt())
                    val text = "$width x $height" + if (start.x % tileSize == 0 && start.y % tileSize == 0) " (snapped)" else ""
                    g2d.drawString(text, x + 5, y - 5)
                }
                if (currentSelection != null) {
                    val sel = currentSelection!!
                    g2d.color = Color(0, 255, 0, 64)
                    g2d.fillRect(sel.x, sel.y, sel.width, sel.height)
                    g2d.color = Color(0, 255, 0)
                    g2d.drawRect(sel.x, sel.y, sel.width, sel.height)
                }
            }
            g2d.scale(1.0 / zoomLevel, 1.0 / zoomLevel)
            g2d.translate(-offsetX, -offsetY)
            g2d.color = Color.WHITE
            g2d.font = Font("Arial", Font.BOLD, 12)
            val zoomText = "Zoom: ${String.format("%.0f", zoomLevel * 100)}%"
            g2d.drawString(zoomText, 10, height - 10)
        }
    }
    inner class TileTypeSwatch(val tileType: TileType) : JPanel() {
        private var selected = false
        init {
            preferredSize = Dimension(30, 30)
            maximumSize = Dimension(30, 30)
            toolTipText = tileType.description
            border = LineBorder(tileType.color.darker(), 1)
            background = tileType.color
            layout = GridBagLayout()
            add(JLabel(tileType.id.toString()).apply {
                foreground = Color.WHITE
                font = Font("Courier New", Font.BOLD, 12)
            })
        }
        fun setSelected(isSelected: Boolean) {
            selected = isSelected
            border = if (selected) {
                LineBorder(Color.WHITE, 2)
            } else {
                LineBorder(tileType.color.darker(), 1)
            }
        }
    }
}
fun main() {
    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        TileMapEditorApp().isVisible = true
    }
}
