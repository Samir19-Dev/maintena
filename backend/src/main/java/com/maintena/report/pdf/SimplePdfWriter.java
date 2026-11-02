package com.maintena.report.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador de PDF muy ligero (sin dependencias externas) para reportes tabulares
 * en tamaño A4 horizontal. Usa las fuentes estándar Helvetica / Helvetica-Bold.
 */
public class SimplePdfWriter {
	private static final float PAGE_WIDTH = 842f;
	private static final float PAGE_HEIGHT = 595f;
	private static final float MARGIN = 36f;
	private static final float HEADER_HEIGHT = 18f;
	private static final float ROW_HEIGHT = 15f;
	private static final float BODY_FONT_SIZE = 8f;
	private static final float HEADER_FONT_SIZE = 8.5f;
	private static final float FOOTER_FONT_SIZE = 8f;

	private final String title;
	private final String subtitle;
	private final String[] headers;
	private final float[] weights;
	private final List<String[]> rows = new ArrayList<>();

	public SimplePdfWriter(String title, String subtitle, String[] headers, float[] weights) {
		if (headers.length != weights.length) {
			throw new IllegalArgumentException("La cantidad de encabezados y anchos debe coincidir");
		}
		this.title = title;
		this.subtitle = subtitle;
		this.headers = headers;
		this.weights = weights;
	}

	public void addRow(String... cells) {
		String[] row = new String[headers.length];
		for (int i = 0; i < headers.length; i++) {
			row[i] = i < cells.length && cells[i] != null ? cells[i] : "";
		}
		rows.add(row);
	}

	public byte[] build() throws IOException {
		float tableWidth = PAGE_WIDTH - (MARGIN * 2);
		float[] widths = new float[weights.length];
		float totalWeight = 0f;
		for (float weight : weights) {
			totalWeight += weight;
		}
		for (int i = 0; i < weights.length; i++) {
			widths[i] = tableWidth * (weights[i] / totalWeight);
		}

		float topContentY = PAGE_HEIGHT - MARGIN - 52f;
		int rowsPerPage = Math.max(1, (int) ((topContentY - 40f) / ROW_HEIGHT));
		int pageCount = Math.max(1, (int) Math.ceil(rows.size() / (double) rowsPerPage));

		List<String> pages = new ArrayList<>();
		for (int page = 0; page < pageCount; page++) {
			int from = page * rowsPerPage;
			int to = Math.min(from + rowsPerPage, rows.size());
			pages.add(buildPage(page, pageCount, widths, rows.subList(from, to)));
		}

		return assemble(pages);
	}

	private String buildPage(int pageIndex, int pageCount, float[] widths, List<String[]> pageRows) {
		StringBuilder sb = new StringBuilder();
		float tableWidth = PAGE_WIDTH - (MARGIN * 2);
		float y = PAGE_HEIGHT - MARGIN;

		sb.append("0.10 0.25 0.45 rg\n");
		sb.append(text("F2", 16, MARGIN, y, title));
		y -= 18f;
		sb.append("0.35 0.35 0.35 rg\n");
		sb.append(text("F1", 9, MARGIN, y, subtitle));
		y -= 24f;

		sb.append("0.90 0.93 0.97 rg\n");
		sb.append(rect(MARGIN, y - 4f, tableWidth, HEADER_HEIGHT, true));
		sb.append("0.10 0.10 0.10 rg\n");

		float x = MARGIN;
		for (int i = 0; i < headers.length; i++) {
			sb.append(text("F2", HEADER_FONT_SIZE, x + 3f, y + 2f, fit(headers[i], widths[i] - 6f, HEADER_FONT_SIZE)));
			x += widths[i];
		}
		y -= HEADER_HEIGHT;

		for (int rowIndex = 0; rowIndex < pageRows.size(); rowIndex++) {
			if (rowIndex % 2 == 1) {
				sb.append("0.96 0.96 0.98 rg\n");
				sb.append(rect(MARGIN, y, tableWidth, ROW_HEIGHT, true));
			}
			sb.append("0.15 0.15 0.15 rg\n");
			x = MARGIN;
			String[] row = pageRows.get(rowIndex);
			for (int col = 0; col < headers.length; col++) {
				sb.append(text("F1", BODY_FONT_SIZE, x + 3f, y + 4f,
					fit(row[col], widths[col] - 6f, BODY_FONT_SIZE)));
				x += widths[col];
			}
			y -= ROW_HEIGHT;
		}

		if (pageRows.isEmpty()) {
			sb.append("0.30 0.30 0.30 rg\n");
			sb.append(text("F1", 10, MARGIN, y, "No hay datos para mostrar con los filtros seleccionados."));
		}

		sb.append("0.45 0.45 0.45 rg\n");
		String footer = "Página " + (pageIndex + 1) + " de " + pageCount;
		sb.append(text("F1", FOOTER_FONT_SIZE, MARGIN, 24f, footer));
		sb.append(text("F1", FOOTER_FONT_SIZE, PAGE_WIDTH - MARGIN - 90f, 24f, "Generado por Maintena"));

		return sb.toString();
	}

	private String text(String font, float size, float x, float y, String value) {
		return "BT /" + font + " " + format(size) + " Tf " + format(x) + " " + format(y)
			+ " Td (" + escape(value) + ") Tj ET\n";
	}

	private String rect(float x, float y, float width, float height, boolean fill) {
		return format(x) + " " + format(y) + " " + format(width) + " " + format(height)
			+ " re " + (fill ? "f" : "S") + "\n";
	}

	private String fit(String value, float maxWidth, float fontSize) {
		if (value == null || value.isBlank()) {
			return "";
		}
		float approxCharWidth = fontSize * 0.52f;
		int maxChars = Math.max(3, (int) (maxWidth / approxCharWidth));
		String clean = value.replace("\r", " ").replace("\n", " ");
		if (clean.length() <= maxChars) {
			return clean;
		}
		return clean.substring(0, maxChars - 3) + "...";
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
	}

	private String format(float value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	private byte[] assemble(List<String> pages) throws IOException {
		int pageObjStart = 5;
		int lastObj = 4 + (pages.size() * 2);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		List<Integer> offsets = new ArrayList<>();
		offsets.add(0);

		write(out, "%PDF-1.4\n");

		StringBuilder kids = new StringBuilder();
		for (int i = 0; i < pages.size(); i++) {
			kids.append(pageObjStart + (i * 2)).append(" 0 R ");
		}

		writeObject(out, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
		writeObject(out, offsets, 2, "<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count "
			+ pages.size() + " >>");
		writeObject(out, offsets, 3,
			"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
		writeObject(out, offsets, 4,
			"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

		for (int i = 0; i < pages.size(); i++) {
			int pageObj = pageObjStart + (i * 2);
			int contentObj = pageObj + 1;
			writeObject(out, offsets, pageObj,
				"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + format(PAGE_WIDTH) + " " + format(PAGE_HEIGHT)
					+ "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentObj + " 0 R >>");

			byte[] content = pages.get(i).getBytes(StandardCharsets.ISO_8859_1);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			write(stream, "<< /Length " + content.length + " >>\nstream\n");
			stream.write(content);
			write(stream, "endstream");

			offsets.add(out.size());
			write(out, contentObj + " 0 obj\n");
			out.write(stream.toByteArray());
			write(out, "\nendobj\n");
		}

		int xrefOffset = out.size();
		StringBuilder xref = new StringBuilder();
		xref.append("xref\n0 ").append(lastObj + 1).append("\n");
		xref.append("0000000000 65535 f \n");
		for (int i = 1; i <= lastObj; i++) {
			int offset = i < offsets.size() ? offsets.get(i) : 0;
			xref.append(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset));
		}
		write(out, xref.toString());
		write(out, "trailer\n<< /Size " + (lastObj + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF");

		return out.toByteArray();
	}

	private void writeObject(ByteArrayOutputStream out, List<Integer> offsets, int number, String body)
		throws IOException {
		offsets.add(out.size());
		write(out, number + " 0 obj\n" + body + "\nendobj\n");
	}

	private void write(ByteArrayOutputStream out, String value) throws IOException {
		out.write(value.getBytes(StandardCharsets.ISO_8859_1));
	}
}
