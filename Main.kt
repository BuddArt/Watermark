package watermark

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import kotlin.system.exitProcess
import java.awt.Color

fun main() {
    val image = getImage()
    val watermark = getImage(true)

    if (!imagesEqualSize(image, watermark)) {
        println("The watermark's dimensions are larger.")
        exitProcess(3)
    }

    var useAlphaChannel = false
    var transparencyColor: Color? = null
    if (hasTransparency(watermark)) {
        useAlphaChannel = checkUseAlphaChannel()
    } else {
        transparencyColor = checkSetTransparencyColor()
    }

    val weight = weightPercent()
    val positionMethod = getPositionMethod()
    var positionForSingle: List<Int> = listOf(0, 0)
    if (positionMethod == "single") {
        positionForSingle = singlePosition(image, watermark)
    }

    val outputName = getOutputName()
    val outputImage = getOutputImage(image, watermark, weight, useAlphaChannel, transparencyColor, positionForSingle, positionMethod)

    ImageIO.write(outputImage, outputName.takeLast(3), File(outputName))
    println("The watermarked image $outputName has been created.")
}

fun getImage(watermark: Boolean = false): BufferedImage {
    println("Input the${ if(watermark) " watermark" else ""} image filename:")
    val imageName = readln()
    val imageFile = File(imageName)
    if (!imageFile.exists()) {
        println("The file $imageName doesn't exist.")
        exitProcess(10)
    }
    val inputImage: BufferedImage = ImageIO.read(imageFile)
    if (!watermark && inputImage.colorModel.numComponents != 3) {
        println("The number of image color components isn't 3.")
        exitProcess(1)
    } else if (watermark && inputImage.colorModel.numComponents < 3) {
        println("The number of watermark color components isn't 3.")
        exitProcess(1)
    }
    if (inputImage.colorModel.pixelSize != 24 && inputImage.colorModel.pixelSize != 32) {
        println("The ${if (watermark) "watermark" else "image"} isn't 24 or 32-bit.")
        exitProcess(2)
    }

    return inputImage
}

fun hasTransparency(inputImage: BufferedImage): Boolean = inputImage.transparency == 3

fun checkUseAlphaChannel(): Boolean {
    println("Do you want to use the watermark's Alpha channel?")
    val useAlphaChannel = readln().lowercase()
    return useAlphaChannel == "yes"
}

fun checkSetTransparencyColor(): Color? {
    println("Do you want to set a transparency color?")
    val setTransparencyColor = readln().lowercase() == "yes"

    if (!setTransparencyColor) { return null }

    try {
        println("Input a transparency color ([Red] [Green] [Blue]):")
        val colors = readln().split(" ").map { it.toInt() }
        assert(colors.size == 3)
        return Color(colors[0], colors[1], colors[2])
    } catch (e: Throwable) {
        println("The transparency color input is invalid.")
        exitProcess(55)
    }
}

fun imagesEqualSize(image: BufferedImage, watermark: BufferedImage): Boolean =
    image.height >= watermark.height && image.width >= watermark.width

fun weightPercent(): Int {
    println("Input the watermark transparency percentage (Integer 0-100):")
    val weight = try {
        readln().toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(5)
    }
    if (weight !in 0..100) {
        println("The transparency percentage is out of range.")
        exitProcess(6)
    }
    return weight
}

fun getOutputName(): String {
    println("Input the output image filename (jpg or png extension):")
    val outputName = readln()
    if (!outputName.contains(".jpg") && !outputName.contains(".png")) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(7)
    }
    return outputName
}

fun getOutputImage(inputImage: BufferedImage, watermarkImage: BufferedImage, weight: Int, useAlphaChannel: Boolean, transparencyColor: Color?, position: List<Int>, positionMethod: String): BufferedImage {
    val outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until inputImage.width) {
        for (y in 0 until inputImage.height) {
            val colorImage = Color(inputImage.getRGB(x, y))
            if (positionMethod == "single") {
                if ((x in position[0] until position[0] + watermarkImage.width) && (y in position[1]until position[1] + watermarkImage.height)) {
                val colorWatermark = Color(watermarkImage.getRGB(x - position[0], y - position[1]), true)
                if (useAlphaChannel && colorWatermark.alpha == 0) {
                    outputImage.setRGB(x, y, colorImage.rgb)
                    continue
                } else if (transparencyColor?.rgb == colorWatermark.rgb) {
                    outputImage.setRGB(x, y, colorImage.rgb)
                    continue
                }
                val newColor = Color(
                    (weight * colorWatermark.red + (100 - weight) * colorImage.red) / 100,
                    (weight * colorWatermark.green + (100 - weight) * colorImage.green) / 100,
                    (weight * colorWatermark.blue + (100 - weight) * colorImage.blue) / 100
                )
                outputImage.setRGB(x, y, newColor.rgb)
                } else outputImage.setRGB(x, y, colorImage.rgb)
            } else if (positionMethod == "grid") {
                val colorWatermark = Color(watermarkImage.getRGB(x % watermarkImage.width, y % watermarkImage.height), true)
                if (useAlphaChannel && colorWatermark.alpha == 0) {
                    outputImage.setRGB(x, y, colorImage.rgb)
                    continue
                } else if (transparencyColor?.rgb == colorWatermark.rgb) {
                    outputImage.setRGB(x, y, colorImage.rgb)
                    continue
                }
                val newColor = Color(
                    (weight * colorWatermark.red + (100 - weight) * colorImage.red) / 100,
                    (weight * colorWatermark.green + (100 - weight) * colorImage.green) / 100,
                    (weight * colorWatermark.blue + (100 - weight) * colorImage.blue) / 100
                )
                outputImage.setRGB(x, y, newColor.rgb)
            }
        }
    }
    return outputImage
}

fun getPositionMethod(): String {
    println("Choose the position method (single, grid):")
    val positionMethod = readln().lowercase()
    if (positionMethod != "single" && positionMethod != "grid") {
        println("The position method input is invalid.")
        exitProcess(66)
    }
    return positionMethod
}

fun singlePosition(image:BufferedImage, watermark: BufferedImage): List<Int> {
    try {
        val diffX = image.width - watermark.width
        val diffY = image.height - watermark.height
        println("Input the watermark position ([x 0-${diffX}] [y 0-${diffY}]):")
        val position = readln().split(" ").map { it.toInt() }
        if (position[0] in 0..diffX && position[1] in 0..diffY) {
            return position
        } else {
            println("The position input is out of range.")
            exitProcess(77)
            return listOf()
        }
    } catch (e: Throwable) {
        println("The position input is invalid.")
        exitProcess(88)
    }
}

/* Моё собственное решение 4 - ого этапа:

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import kotlin.system.exitProcess
import java.awt.Color

var transparent: String = "no"
var weight: Int = 0
var trColorRGB: List<String> = listOf()

fun main() {
    println("Input the image filename:")
    val inputImageName = readln()
    val inputImageFile = File(inputImageName)

    checkFile(inputImageFile, inputImageName)

    val inputImage: BufferedImage = ImageIO.read(inputImageFile)
    if (inputImage.colorModel.numColorComponents != 3) {
        println("The number of image color components isn't 3.")
        exitProcess(1)
    }
    if (inputImage.colorModel.pixelSize != 24 && inputImage.colorModel.pixelSize != 32) {
        println("The image isn't 24 or 32-bit.")
        exitProcess(2)
    }

    println("Input the watermark image filename:")
    val inputWatermarkName = readln()
    val inputWatermarkFile = File(inputWatermarkName)

    checkFile(inputWatermarkFile, inputWatermarkName)

    val watermarkImage: BufferedImage = ImageIO.read(inputWatermarkFile)
    if (watermarkImage.colorModel.numColorComponents != 3) {
        println("The number of watermark color components isn't 3.")
        exitProcess(11)
    }
    if (watermarkImage.colorModel.pixelSize != 24 && watermarkImage.colorModel.pixelSize != 32) {
        println("The watermark isn't 24 or 32-bit.")
        exitProcess(22)
    }
    if (inputImage.width * inputImage.height != watermarkImage.width * watermarkImage.height) {
        println("The image and watermark dimensions are different.")
        exitProcess(3)
    }

    if (watermarkImage.transparency == 3) {
        if (watermarkImage.transparency == 3) {
            println("Do you want to use the watermark's Alpha channel?")
            transparent = readln().lowercase()
        }

        percentWeight()

        createOutputFile(inputImage, watermarkImage)
    } else {
        println("Do you want to set a transparency color?")
        val answerSetTransparency = readln().lowercase()
        if (answerSetTransparency != "yes") {
            percentWeight()
            createOutputFile(inputImage, watermarkImage)
        } else {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            try {
                trColorRGB = readln().split(" ")
                if (trColorRGB[0].toInt() in 0..255 && trColorRGB[1].toInt() in 0..255 && trColorRGB[2].toInt() in 0..255 && trColorRGB.size == 3) {
                    percentWeight()

                    println("Input the output image filename (jpg or png extension):")
                    val outputImageName = readln()
                    var razr = ""
                    razr = if (outputImageName.contains(".jpg")) {
                        "jpg"
                    } else if (outputImageName.contains(".png")) {
                        "png"
                    } else {
                        println("The output file extension isn't \"jpg\" or \"png\".")
                        exitProcess(7)
                    }
                    val outputImageFile = File(outputImageName)
                    val outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_RGB)
                    for (x in 0 until inputImage.width) {
                        for (y in 0 until inputImage.height) {
                            val colorImage = Color(inputImage.getRGB(x, y))
                            val colorWatermark = Color(watermarkImage.getRGB(x, y), true)
                            val newColor = Color(
                                (weight * colorWatermark.red + (100 - weight) * colorImage.red) / 100,
                                (weight * colorWatermark.green + (100 - weight) * colorImage.green) / 100,
                                (weight * colorWatermark.blue + (100 - weight) * colorImage.blue) / 100,
                            )
                            if (colorWatermark.red == trColorRGB[0].toInt() && colorWatermark.green == trColorRGB[1].toInt() && colorWatermark.blue == trColorRGB[2].toInt()) {
                                outputImage.setRGB(x, y, colorImage.rgb)
                            } else outputImage.setRGB(x, y, newColor.rgb)
                        }
                    }
                    saveImage(outputImage, razr, outputImageFile)
                    println("The watermarked image $outputImageName has been created.")
                } else {
                    println("The transparency color input is invalid.")
                    exitProcess(55)
                }
            } catch (e: Exception) {
                when(e) {
                    is IndexOutOfBoundsException, is NumberFormatException -> {
                        println("The transparency color input is invalid.")
                        exitProcess(55)
                    }
                }
            }
        }
    }
}

fun saveImage(image: BufferedImage, razr: String, imageFile: File) {
    ImageIO.write(image, razr, imageFile)
}

fun checkFile(inputFile: File, inputName: String) {
    if (!inputFile.exists()) {
        println("The file $inputName doesn't exist.")
        exitProcess(10)
    }
}

fun createOutputFile (inputImage: BufferedImage, watermarkImage: BufferedImage) {
    println("Input the output image filename (jpg or png extension):")
    val outputImageName = readln()
    var razr = ""
    razr = if (outputImageName.contains(".jpg")) {
        "jpg"
    } else if (outputImageName.contains(".png")) {
        "png"
    } else {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(7)
    }
    val outputImageFile = File(outputImageName)
    val outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until inputImage.width) {
        for (y in 0 until inputImage.height) {
            val colorImage = Color(inputImage.getRGB(x, y))
            val colorWatermark = Color(watermarkImage.getRGB(x, y), true)
            val newColor = Color(
                (weight * colorWatermark.red + (100 - weight) * colorImage.red) / 100,
                (weight * colorWatermark.green + (100 - weight) * colorImage.green) / 100,
                (weight * colorWatermark.blue + (100 - weight) * colorImage.blue) / 100,
            )
            if (watermarkImage.transparency == 3 && transparent == "yes" && colorWatermark.alpha == 0) {
                outputImage.setRGB(x, y, colorImage.rgb)
            } else outputImage.setRGB(x, y, newColor.rgb)
        }
    }
    saveImage(outputImage, razr, outputImageFile)
    println("The watermarked image $outputImageName has been created.")
}

fun percentWeight() {
    println("Input the watermark transparency percentage (Integer 0-100):")
    try {
        weight = readln().toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(5)
    }
    if (weight !in 0..100) {
        println("The transparency percentage is out of range.")
        exitProcess(6)
    }
}
 */