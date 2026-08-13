#!/usr/bin/env python3
"""Generate the public invoice and purchase-order fixtures used by the demo."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
PUBLIC_DIR = ROOT / "frontend" / "public" / "samples"
FONT_PATH = "/System/Library/Fonts/Supplemental/AppleGothic.ttf"
FONT_NAME = "AppleGothic"


def draw_pdf_invoice(path: Path) -> None:
    pdfmetrics.registerFont(TTFont(FONT_NAME, FONT_PATH))
    page_width, page_height = A4
    doc = canvas.Canvas(str(path), pagesize=A4)

    doc.setFillColor(colors.HexColor("#173C8F"))
    doc.rect(0, page_height - 118, page_width, 118, stroke=0, fill=1)
    doc.setFillColor(colors.white)
    doc.setFont(FONT_NAME, 26)
    doc.drawString(42, page_height - 64, "운송 송장")
    doc.setFont(FONT_NAME, 11)
    doc.drawString(42, page_height - 91, "KOLOG SMART MOBILITY · DEMO INVOICE")

    doc.setFillColor(colors.HexColor("#172033"))
    doc.setFont(FONT_NAME, 12)
    doc.drawRightString(page_width - 42, page_height - 145, "송장번호  INV-001")
    doc.setStrokeColor(colors.HexColor("#DCE4F2"))
    doc.line(42, page_height - 162, page_width - 42, page_height - 162)

    rows = [
        ("품목", "냉동 닭가슴살"),
        ("수량", "100BOX"),
        ("총중량", "500kg"),
        ("보관 및 운송온도", "-18℃ 이하"),
        ("출발지", "부산"),
        ("도착지", "서울"),
        ("희망 출발일", "2026-09-01"),
        ("화물가액", "3,000,000원"),
    ]
    y = page_height - 205
    for label, value in rows:
        doc.setFillColor(colors.HexColor("#F3F6FC"))
        doc.roundRect(42, y - 30, 150, 44, 6, stroke=0, fill=1)
        doc.setFillColor(colors.HexColor("#66748A"))
        doc.setFont(FONT_NAME, 10)
        doc.drawString(56, y - 12, label)
        doc.setFillColor(colors.HexColor("#172033"))
        doc.setFont(FONT_NAME, 13)
        doc.drawString(215, y - 13, value)
        doc.setStrokeColor(colors.HexColor("#E8EDF5"))
        doc.line(205, y - 30, page_width - 42, y - 30)
        y -= 55

    doc.setFillColor(colors.HexColor("#EAF1FF"))
    doc.roundRect(42, 60, page_width - 84, 58, 10, stroke=0, fill=1)
    doc.setFillColor(colors.HexColor("#173C8F"))
    doc.setFont(FONT_NAME, 10)
    doc.drawString(58, 93, "시연용 샘플 문서")
    doc.setFillColor(colors.HexColor("#4F5F78"))
    doc.drawString(58, 75, "실제 거래나 세금 증빙에 사용할 수 없습니다.")
    doc.save()


def draw_png_purchase_order(path: Path) -> None:
    width, height = 1240, 1754
    image = Image.new("RGB", (width, height), "#F3F6FC")
    draw = ImageDraw.Draw(image)
    title_font = ImageFont.truetype(FONT_PATH, 58)
    section_font = ImageFont.truetype(FONT_PATH, 31)
    label_font = ImageFont.truetype(FONT_PATH, 24)
    value_font = ImageFont.truetype(FONT_PATH, 30)
    small_font = ImageFont.truetype(FONT_PATH, 21)

    draw.rounded_rectangle((70, 65, width - 70, height - 65), radius=34, fill="white")
    draw.rounded_rectangle((70, 65, width - 70, 280), radius=34, fill="#173C8F")
    draw.rectangle((70, 190, width - 70, 280), fill="#173C8F")
    draw.text((120, 112), "발 주 서", font=title_font, fill="white")
    draw.text((120, 204), "KOLOG SMART MOBILITY · DEMO PURCHASE ORDER", font=small_font, fill="#D7E4FF")

    draw.text((120, 335), "발주 기본정보", font=section_font, fill="#172033")
    draw.text((805, 340), "발주번호  PO-002", font=label_font, fill="#52617A")
    draw.line((120, 395, width - 120, 395), fill="#DCE4F2", width=3)

    rows = [
        ("품목", "생수"),
        ("수량", "1,000BOX"),
        ("총중량", "1,000kg"),
        ("온도조건", "상온"),
        ("출발지", "대전"),
        ("도착지", "부산"),
        ("희망납품일", "2026-09-02"),
        ("화물가액", "12,000,000원"),
    ]
    y = 445
    for label, value in rows:
        draw.rounded_rectangle((120, y, 395, y + 76), radius=12, fill="#F3F6FC")
        draw.text((148, y + 22), label, font=label_font, fill="#64738A")
        draw.text((445, y + 18), value, font=value_font, fill="#172033")
        draw.line((425, y + 76, width - 120, y + 76), fill="#E8EDF5", width=2)
        y += 96

    draw.rounded_rectangle((120, 1315, width - 120, 1510), radius=22, fill="#EAF1FF")
    draw.text((155, 1350), "납품 요청사항", font=section_font, fill="#173C8F")
    draw.text((155, 1410), "파손 방지 포장 · 직사광선 노출 금지", font=value_font, fill="#384966")
    draw.text((155, 1460), "희망납품일은 희망출발일과 다를 수 있습니다.", font=small_font, fill="#64738A")
    draw.text((120, 1585), "시연용 샘플 문서 · 실제 발주 증빙으로 사용할 수 없습니다.", font=small_font, fill="#8290A5")
    image.save(path, format="PNG", optimize=True)


def main() -> None:
    PUBLIC_DIR.mkdir(parents=True, exist_ok=True)
    invoice = PUBLIC_DIR / "INV_001_냉동닭가슴살.pdf"
    purchase_order = PUBLIC_DIR / "PO_002_생수.png"
    draw_pdf_invoice(invoice)
    draw_png_purchase_order(purchase_order)
    print(invoice)
    print(purchase_order)


if __name__ == "__main__":
    main()
