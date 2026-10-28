#!/usr/bin/env python3
"""Seed the MediQuery demo knowledge base via POST /api/ingest."""
import json
import urllib.request

BASE = "http://localhost:8080"

DOCS = [
    {
        "docId": "t2d-overview",
        "text": (
            "Type 2 diabetes is a chronic condition in which the body becomes resistant "
            "to insulin or does not produce enough insulin, leading to high blood sugar. "
            "Common symptoms include increased thirst and hunger, frequent urination, "
            "fatigue, and blurred vision. Some people experience slow healing of cuts "
            "and wounds. In later stages, numbness or tingling in the hands and feet "
            "may occur, along with recurring skin, gum, or bladder infections. Many "
            "people have no symptoms in the early stages. Risk factors include excess "
            "weight, inactivity, family history, and age over 45. Management typically "
            "involves lifestyle changes, blood sugar monitoring, and medication such "
            "as metformin when prescribed."
        ),
    },
    {
        "docId": "hypertension-basics",
        "text": (
            "Hypertension, or high blood pressure, is often called the silent killer "
            "because it usually has no symptoms. It is diagnosed when readings are "
            "consistently at or above 130/80 mmHg. Risk factors include high salt "
            "intake, obesity, smoking, excessive alcohol, chronic stress, and family "
            "history. Long-term complications can include heart disease, stroke, and "
            "kidney damage. First-line management is lifestyle change: reduced sodium, "
            "regular exercise, weight control, and limited alcohol. When lifestyle "
            "measures are not enough, doctors may prescribe ACE inhibitors, calcium "
            "channel blockers, or diuretics."
        ),
    },
    {
        "docId": "cold-vs-flu",
        "text": (
            "The common cold and influenza (flu) are both respiratory illnesses but are "
            "caused by different viruses. Colds usually bring a runny or stuffy nose, "
            "sneezing, and a mild sore throat, developing gradually. Flu tends to hit "
            "suddenly with high fever, body aches, chills, and pronounced fatigue. "
            "Most colds resolve within 7 to 10 days with rest and fluids. Flu can lead "
            "to serious complications such as pneumonia, especially in older adults, "
            "young children, and people with chronic conditions. Annual flu vaccination "
            "is the most effective prevention. Antiviral drugs may help if started "
            "within 48 hours of flu symptoms."
        ),
    },
]

for doc in DOCS:
    req = urllib.request.Request(
        BASE + "/api/ingest",
        data=json.dumps(doc).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as resp:
        print(resp.status, resp.read().decode()[:120])
print("done")
