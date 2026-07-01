package PDVGUI.utils;

import com.compomics.util.enumeration.ImageType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Export to figure formats for Components.
 * Created by Ken on 12/1/2017.
 */
public class Export {

    /**
     * Constructor
     * @param component Component to export
     * @param bounds Dimensions of the viewport
     * @param exportFile Output file
     * @param imageType Image type
     */
    public static void exportPic(Component component, Rectangle bounds, File exportFile, ImageType imageType) throws IOException, TranscoderException {

        if (imageType == ImageType.PDF){
            exportPDF(component, bounds, exportFile);
        } else if (imageType == ImageType.PNG){
            exportPNG(component, bounds, exportFile);
        } else if (imageType == ImageType.TIFF){
            exportTIFF(component, bounds, exportFile);
        } else if (imageType == ImageType.SVG){
            exportSVG(component, bounds, exportFile);
        }
    }

    /**
     * Export a component at a target resolution. For raster formats (PNG, TIFF) the
     * component is re-rendered at dpi/96 scale, so the output is genuinely higher
     * resolution rather than an upscaled bitmap, and the chosen dpi is embedded in
     * the file. Vector formats (PDF, SVG) are resolution-independent, so dpi is ignored.
     * @param component Component to export
     * @param bounds Dimensions of the viewport
     * @param exportFile Output file
     * @param imageType Image type
     * @param dpi Target dots per inch for raster formats
     */
    public static void exportPic(Component component, Rectangle bounds, File exportFile, ImageType imageType, int dpi)
            throws IOException, TranscoderException {

        if (imageType == ImageType.PNG){
            exportPNG(component, bounds, exportFile, dpi / 96.0, dpi);
        } else if (imageType == ImageType.TIFF){
            exportTIFF(component, bounds, exportFile, dpi / 96.0, dpi);
        } else {
            exportPic(component, bounds, exportFile, imageType);
        }
    }

    /**
     * Constructor
     * @param chart JFreeChart to export
     * @param bounds Dimensions of the viewport
     * @param exportFile Output file
     * @param imageType Image type
     */
    public static void exportPic(JFreeChart chart, Rectangle bounds, File exportFile, ImageType imageType)
            throws IOException, TranscoderException {

        DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        SVGDocument svgDocument = (SVGDocument) domImplementation.createDocument(svgNS, "svg", null);

        // Render text as vector outlines (not <text> elements). The old FOP-based pdf-transcoder did
        // this implicitly, so the exported font looked like the on-screen Arial; modern FOP instead
        // substitutes a base-14 font (Times) for an unresolved family. Outlines keep the exact look.
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(SVGGeneratorContext.createDefault(svgDocument), true);
        svgGraphics2D.setSVGCanvasSize(bounds.getSize());

        chart.draw(svgGraphics2D, bounds);

        exportExceptedFormatPic(exportFile, imageType, svgGraphics2D);
    }

    /**
     * Exports the selected file to the selected format.
     * @param exportFile Output file
     * @param imageType Image type
     * @param svgGraphics2D SVGGraphics2D
     */
    private static void exportExceptedFormatPic(File exportFile, ImageType imageType, SVGGraphics2D svgGraphics2D)
            throws IOException, TranscoderException {

        DecimalFormat df = DecimalFormats.create("#.000000");

        if (new File(exportFile.getAbsolutePath() + ".temp").exists()) {
            new File(exportFile.getAbsolutePath() + ".temp").delete();
        }

        File svgFile = exportFile;

        if (imageType != ImageType.SVG) {
            svgFile = new File(exportFile.getAbsolutePath() + ".temp");
        }

        OutputStream outputStream = new FileOutputStream(svgFile);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        Writer out = new OutputStreamWriter(bos, "UTF-8");

        svgGraphics2D.stream(out, true);
        outputStream.flush();
        outputStream.close();
        //out.close();
        bos.close();

        if (imageType != ImageType.SVG) {
            String svgURI = svgFile.toURI().toString();
            TranscoderInput svgInputFile = new TranscoderInput(svgURI);

            OutputStream outstream = new FileOutputStream(exportFile);
            bos = new BufferedOutputStream(outstream);
            TranscoderOutput output = new TranscoderOutput(bos);

            if (imageType == ImageType.PDF) {

                Transcoder pdfTranscoder = new PDFTranscoder();
                // Size the PDF page with the pixel-to-mm hint (as PNG/TIFF do). KEY_DEVICE_RESOLUTION
                // does NOT control the page size, leaving it scaled by the screen DPI (2x too large
                // on a HiDPI display).
                pdfTranscoder.addTranscodingHint(PDFTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, Float.valueOf(df.format(25.4 / Toolkit.getDefaultToolkit().getScreenResolution())));
                pdfTranscoder.transcode(svgInputFile, output);

            } else if (imageType == ImageType.JPEG) {

                Transcoder tiffTranscoder = new TIFFTranscoder();
                tiffTranscoder.addTranscodingHint(TIFFTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, Float.valueOf(df.format(25.4 / Toolkit.getDefaultToolkit().getScreenResolution())));
                tiffTranscoder.addTranscodingHint(TIFFTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true);
                tiffTranscoder.transcode(svgInputFile, output);

            } else if (imageType == ImageType.PNG) {

                Transcoder pngTranscoder = new PNGTranscoder();
                pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, Float.valueOf(df.format(25.4 / Toolkit.getDefaultToolkit().getScreenResolution())));
                pngTranscoder.transcode(svgInputFile, output);

            } else if (imageType == ImageType.SVG) {

            }

            outstream.flush();
            outstream.close();
            bos.close();

            if (svgFile.exists()) {
                svgFile.delete();
            }
        }
    }

    /**
     * Export component to pdf format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     * @throws TranscoderException
     */
    private static void exportPDF(Component component, Rectangle bounds, File exportFile) throws IOException, TranscoderException {

        DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        SVGDocument svgDocument = (SVGDocument) domImplementation.createDocument(svgNS, "svg", null);

        // Render text as vector outlines (not <text> elements). The old FOP-based pdf-transcoder did
        // this implicitly, so the exported font looked like the on-screen Arial; modern FOP instead
        // substitutes a base-14 font (Times) for an unresolved family. Outlines keep the exact look.
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(SVGGeneratorContext.createDefault(svgDocument), true);
        svgGraphics2D.setSVGCanvasSize(bounds.getSize());

        component.paintAll(svgGraphics2D);

        if (new File(exportFile.getAbsolutePath() + ".temp").exists()) {
            new File(exportFile.getAbsolutePath() + ".temp").delete();
        }

        File svgFile = new File(exportFile.getAbsolutePath() + ".temp");

        OutputStream outputStream = new FileOutputStream(svgFile);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        Writer out = new OutputStreamWriter(bos, "UTF-8");

        svgGraphics2D.stream(out, true);
        outputStream.flush();
        outputStream.close();
        out.close();
        bos.close();

        String svgURI = svgFile.toURI().toString();
        TranscoderInput svgInputFile = new TranscoderInput(svgURI);

        OutputStream outstream = new FileOutputStream(exportFile);
        bos = new BufferedOutputStream(outstream);
        TranscoderOutput output = new TranscoderOutput(bos);

        Transcoder pdfTranscoder = new PDFTranscoder();
        // Size the PDF page with the pixel-to-mm hint (as PNG/TIFF do). KEY_DEVICE_RESOLUTION does
        // NOT control the page size, leaving it scaled by the screen DPI (2x too large on HiDPI).
        pdfTranscoder.addTranscodingHint(PDFTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, (float) (25.4 / Toolkit.getDefaultToolkit().getScreenResolution()));
        pdfTranscoder.transcode(svgInputFile, output);

        outstream.flush();
        outstream.close();
        bos.close();

        if (svgFile.exists()) {
            svgFile.delete();
        }

    }

    /**
     * Export component to pdf format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     * @throws TranscoderException
     */
    private static void exportSVG(Component component, Rectangle bounds, File exportFile) throws IOException, TranscoderException {

        DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        SVGDocument svgDocument = (SVGDocument) domImplementation.createDocument(svgNS, "svg", null);

        // Render text as vector outlines (not <text> elements). The old FOP-based pdf-transcoder did
        // this implicitly, so the exported font looked like the on-screen Arial; modern FOP instead
        // substitutes a base-14 font (Times) for an unresolved family. Outlines keep the exact look.
        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(SVGGeneratorContext.createDefault(svgDocument), true);
        svgGraphics2D.setSVGCanvasSize(bounds.getSize());

        component.paintAll(svgGraphics2D);

        if (new File(exportFile.getAbsolutePath() + ".temp").exists()) {
            new File(exportFile.getAbsolutePath() + ".temp").delete();
        }

        OutputStream outputStream = new FileOutputStream(exportFile);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        Writer out = new OutputStreamWriter(bos, "UTF-8");

        svgGraphics2D.stream(out, true);
        outputStream.flush();
        outputStream.close();
        out.close();
        bos.close();
    }

    /**
     * Export component to png format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     */
    private static void exportPNG(Component component, Rectangle bounds, File exportFile) throws IOException {
        exportPNG(component, bounds, exportFile, 1.0, Toolkit.getDefaultToolkit().getScreenResolution());
    }

    /**
     * Export component to png format at a given render scale and embedded DPI
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @param scale Render scale (output pixels = bounds * scale)
     * @param dpi Dots per inch to embed
     * @throws IOException
     */
    private static void exportPNG(Component component, Rectangle bounds, File exportFile, double scale, int dpi) throws IOException {

        // Use floor so the image is never wider/taller than the painted (scaled) content; rounding up
        // would leave an unpainted sliver that shows as a black line on the right/bottom edge.
        int width = Math.max(1, (int) Math.floor(bounds.width * scale));
        int height = Math.max(1, (int) Math.floor(bounds.height * scale));

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = bufferedImage.createGraphics();

        if (scale != 1.0) {
            g.scale(scale, scale);
        }

        component.paint(g);

        g.dispose();

        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext();){

            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()){
                continue;
            }
            ImageOutputStream stream = null;
            try {

                setDPI(metadata, dpi);

                stream = ImageIO.createImageOutputStream(exportFile);
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(bufferedImage, null, metadata), writeParam);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stream.close();
            }
            break;
        }
    }

    /**
     * Set DPI for png format pic
     * @param metadata IIOMetadata
     * @throws IIOInvalidTreeException
     */
    private static void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {
        setDPI(metadata, Toolkit.getDefaultToolkit().getScreenResolution());
    }

    /**
     * Set DPI for png format pic
     * @param metadata IIOMetadata
     * @param dpi Dots per inch to embed
     * @throws IIOInvalidTreeException
     */
    private static void setDPI(IIOMetadata metadata, int dpi) throws IIOInvalidTreeException {

        double dotsPerMilli = 1.0 * dpi / 10 / 2.54;

        IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
        horizontalPixelSize.setAttribute("value", String.valueOf(dotsPerMilli));

        IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
        verticalPixelSize.setAttribute("value", String.valueOf(dotsPerMilli));

        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        dimension.appendChild(horizontalPixelSize);
        dimension.appendChild(verticalPixelSize);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dimension);

        metadata.mergeTree("javax_imageio_1.0", root);

    }

    /**
     * Export component to tiff format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     */
    private static void exportTIFF(Component component, Rectangle bounds, File exportFile) throws IOException {
        exportTIFF(component, bounds, exportFile, 1.0, Toolkit.getDefaultToolkit().getScreenResolution());
    }

    /**
     * Export component to tiff format at a given render scale and embedded DPI
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @param scale Render scale (output pixels = bounds * scale)
     * @param dpi Dots per inch to embed
     * @throws IOException
     */
    private static void exportTIFF(Component component, Rectangle bounds, File exportFile, double scale, int dpi) throws IOException {

        // Use floor so the image is never wider/taller than the painted (scaled) content; rounding up
        // would leave an unpainted sliver that shows as a black line on the right/bottom edge.
        int width = Math.max(1, (int) Math.floor(bounds.width * scale));
        int height = Math.max(1, (int) Math.floor(bounds.height * scale));

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = bufferedImage.createGraphics();

        if (scale != 1.0) {
            g.scale(scale, scale);
        }

        component.paint(g);

        g.dispose();

        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("tiff"); iw.hasNext();) {

            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            //Picture compression
            //writeParam.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
            //writeParam.setCompressionType("CCITT T.6");
            //writeParam.setCompressionQuality(0.2f);

            ArrayList<Entry> entries = new ArrayList<>();

            entries.add(new TIFFEntry(TIFF.TAG_X_RESOLUTION, new Rational(dpi)));
            entries.add(new TIFFEntry(TIFF.TAG_Y_RESOLUTION, new Rational(dpi)));

            TIFFImageMetadata tiffMetadata = new TIFFImageMetadata(entries);

            ImageOutputStream stream = null;

            try {
                stream = ImageIO.createImageOutputStream(exportFile);
                writer.setOutput(stream);

                writer.write(null, new IIOImage(bufferedImage, null, tiffMetadata), writeParam);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stream.close();
            }
            break;

        }

    }

    /**
     * Export a component as a PNG with a thin border and an optional drop shadow on a
     * transparent background, re-rendered at the given DPI. Mirrors the framed window
     * image export in CasanovoGUI.
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @param dpi Target dots per inch
     * @param shadow Whether to draw a drop shadow behind the framed content
     * @throws IOException
     */
    public static void exportFramedPNG(Component component, Rectangle bounds, File exportFile, int dpi, boolean shadow) throws IOException {

        double scale = dpi / 96.0;
        int contentWidth = Math.max(1, (int) Math.floor(bounds.width * scale));
        int contentHeight = Math.max(1, (int) Math.floor(bounds.height * scale));

        BufferedImage content = new BufferedImage(contentWidth, contentHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D contentGraphics = content.createGraphics();
        contentGraphics.scale(scale, scale);
        component.paint(contentGraphics);
        contentGraphics.dispose();

        int border = Math.max(1, (int) Math.round(scale));
        int margin = shadow ? (int) Math.round(28 * scale) : 0;
        int cardWidth = contentWidth + 2 * border;
        int cardHeight = contentHeight + 2 * border;
        int totalWidth = cardWidth + 2 * margin;
        int totalHeight = cardHeight + 2 * margin;

        BufferedImage output = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (shadow) {
            BufferedImage shadowImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D shadowGraphics = shadowImage.createGraphics();
            shadowGraphics.setColor(new Color(0, 0, 0, 90));
            shadowGraphics.fillRect(margin, margin + (int) Math.round(5 * scale), cardWidth, cardHeight);
            shadowGraphics.dispose();
            // Cap the blur radius so very high-DPI exports stay fast (the shadow is cosmetic).
            int blurRadius = Math.min((int) Math.round(14 * scale), 60);
            g.drawImage(gaussianBlur(shadowImage, blurRadius), 0, 0, null);
        }

        g.setColor(new Color(0xC8, 0xC8, 0xC8));
        g.fillRect(margin, margin, cardWidth, cardHeight);
        g.drawImage(content, margin + border, margin + border, null);
        g.dispose();

        writePNG(output, dpi, exportFile);
    }

    /**
     * Write a BufferedImage to PNG with the given DPI embedded.
     * @param image Image to write
     * @param dpi Dots per inch to embed
     * @param exportFile Export file
     * @throws IOException
     */
    private static void writePNG(BufferedImage image, int dpi, File exportFile) throws IOException {

        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext();){

            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromRenderedImage(image);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()){
                continue;
            }
            ImageOutputStream stream = null;
            try {

                setDPI(metadata, dpi);

                stream = ImageIO.createImageOutputStream(exportFile);
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(image, null, metadata), writeParam);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stream.close();
            }
            break;
        }
    }

    /**
     * Apply a separable Gaussian blur to an image (used for the drop shadow).
     * @param source Source image
     * @param radius Blur radius in pixels
     * @return Blurred image
     */
    private static BufferedImage gaussianBlur(BufferedImage source, int radius) {

        if (radius < 1) {
            return source;
        }

        float[] kernelData = gaussianKernel(radius);

        ConvolveOp horizontal = new ConvolveOp(new Kernel(kernelData.length, 1, kernelData), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, kernelData.length, kernelData), ConvolveOp.EDGE_NO_OP, null);

        return vertical.filter(horizontal.filter(source, null), null);
    }

    /**
     * Build a normalized 1-D Gaussian kernel of the given radius.
     * @param radius Blur radius in pixels
     * @return Normalized kernel weights
     */
    private static float[] gaussianKernel(int radius) {

        int size = radius * 2 + 1;
        float[] data = new float[size];
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2 * sigma * sigma;
        float sum = 0;

        for (int i = 0; i < size; i++) {
            int x = i - radius;
            data[i] = (float) Math.exp(-(x * x) / twoSigmaSquare);
            sum += data[i];
        }
        for (int i = 0; i < size; i++) {
            data[i] /= sum;
        }
        return data;
    }

}
