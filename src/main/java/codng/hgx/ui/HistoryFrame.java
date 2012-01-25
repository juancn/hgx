		pw.printf(HEADER_ROW, "Summary:", "<b>" + Colorizer.htmlEscape(row.changeSet.summary) + "</b>");
			Colorizer colorizer = Colorizer.PLAIN;
				final String line = Colorizer.htmlEscape(rawLine);
					final String file = matcher.group(2);
					if(file.endsWith(".java")) {
						colorizer = new JavaColorizer();
					} else {
						colorizer = Colorizer.PLAIN;
					}
					pw.printf("<span style=\"font-size: 8px;\">(%4d|    )</span><span style=\"background: rgb(255,238,238);\">%s</span>\n", oldStart, colorizer.colorizeLine(rawLine));
					pw.printf("<span style=\"font-size: 8px;\">(    |%4d)</span><span style=\"background: rgb(221,255,221);\">%s</span>\n", newStart, colorizer.colorizeLine(rawLine));
					pw.printf("<span style=\"font-size: 8px;\">(%4d|%4d)</span><span>%s</span>\n",oldStart, newStart, colorizer.colorizeLine(rawLine));