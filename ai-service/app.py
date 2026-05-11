from flask import Flask, request, jsonify, stream_with_context, Response
import time

app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status":"ok"}), 200

@app.route('/describe', methods=['POST'])
def describe():
    data = request.json or {}
    text = data.get('text', '')
    # Minimal placeholder implementation that would call Groq API in production
    resp = {
        "confidence": 0.75,
        "model_used": "groq-llama-3.3-70b",
        "response_time_ms": 25,
        "cached": False,
        "description": f"AI-generated description for: {text[:200]}"
    }
    return jsonify(resp)

@app.route('/generate-report', methods=['POST'])
def generate_report():
    data = request.json or {}
    # Stream a simple report for demonstration
    def generate():
        for i in range(3):
            chunk = {"progress": (i+1)/3, "message": f"Generating part {i+1}/3"}
            yield f"data: {jsonify(chunk).get_data(as_text=True)}\n\n"
            time.sleep(0.2)
        final = {"confidence":0.8, "model_used":"groq-llama-3.3-70b", "response_time_ms": 600, "cached": False, "report":"Report content..."}
        yield f"data: {jsonify(final).get_data(as_text=True)}\n\n"
    return Response(stream_with_context(generate()), mimetype='text/event-stream')

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
