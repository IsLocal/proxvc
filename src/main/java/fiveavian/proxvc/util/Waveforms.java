package fiveavian.proxvc.util;

import fiveavian.proxvc.ProxVCClient;
import net.minecraft.client.render.tessellator.Tessellator;
import net.minecraft.core.util.helper.MathHelper;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.HashMap;





public class Waveforms {
    public enum types {
        OFF,
        BASIC,
        PARTICLE,
        GLOW,
        DOT,
        SPECTRUM,
        RING
    }
    private static final int[] blankPoints = new int[20];


    public static int[] getWaveformPoints(ByteBuffer samples, int numPoints) {
        if (samples == null) {
            return blankPoints;
        }
        int sampleCount = samples.remaining() / 2; // 16-bit samples
        int[] points = new int[numPoints];
        for (int i = 0; i < numPoints; i++) {
            int sampleIndex = i * sampleCount / numPoints;
            short sample = samples.getShort(sampleIndex * 2);
            points[i] = sample; // Scale as needed for rendering
        }
        return points;
    }

    public static void renderWaveformStyle(Waveforms.types type,int[] points, float x, float y, float width, float height, float alpha, boolean drawBackground, Float distanceFromCamera) {
        if (points == null) {
            points = blankPoints;
        }
        if (type == null) {
            type = ProxVCClient.instance.waveformType.value;
        }
        switch (type) {
            case OFF: return;
            case PARTICLE:

                renderParticleWaveform(points, x, y, width, height, alpha);

                break;
            case GLOW:
                if (drawBackground) {
                    drawBackground(x, y, width, height, alpha * alpha);
                }
                renderGlowWaveform(points, x, y, width, height, alpha);
                break;
            case DOT:
                if (drawBackground) {
                    drawBackground(x, y, width, height, alpha);
                }
                renderDotWaveform(points, x, y, width, height, alpha);
                break;
            case SPECTRUM:
                if (drawBackground) {
                    drawBackground(x, y, width, height, alpha);
                }
                renderSpectrumWaveform(points, x, y, width, height, alpha);
                break;
            case RING:
                renderRingWaveform(points, x, y, width, height, alpha);
                break;
            default:
                if (drawBackground) {
                    drawBackground(x, y, width, height, alpha);
                }
                renderWaveform(points, x, y, width, height, alpha);
        }
    }

    private static void drawBackground(float x, float y, float width, float height, float alpha) {

        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        tessellator.addVertex(x - 1, y - 1, 0.0);
        tessellator.addVertex(x - 1, y + height + 1, 0.0);
        tessellator.addVertex(x + width + 1, y + height + 1, 0.0);
        tessellator.addVertex(x + width + 1, y - 1, 0.0);
        tessellator.draw();

    }
    private static void renderRingWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        float centerX = x + width/2;
        float centerY = y + height/2;
        float maxRadius = Math.min(width, height) / 3;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // Draw concentric rings
        for (int i = 0; i < n; i += 2) {
            float amplitude = Math.abs(points[i]) / 32768.0f;
            float radius = maxRadius * ((float)i/n + amplitude * 0.3f);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glColor4f(0.3f + amplitude * 0.7f, 0.5f, 1.0f, alpha * (1.0f - (float)i/n));

            for (int j = 0; j < 32; j++) {
                float angle = (float)(j * Math.PI * 2 / 32);
                float distortion = amplitude * (float)Math.sin(angle * 3 + System.currentTimeMillis() / 200.0f) * radius * 0.2f;
                float px = centerX + (float)Math.cos(angle) * (radius + distortion);
                float py = centerY + (float)Math.sin(angle) * (radius + distortion);
                GL11.glVertex2f(px, py);
            }
            GL11.glEnd();
        }

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void  renderSpectrumWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        float barWidth = width / n - 1;

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // Draw main bars
        for (int i = 0; i < n; i++) {
            float px = x + (i * (barWidth + 1));
            float amplitude = Math.abs(points[i]) / 32768.0f;

            // Draw segments with different colors
            for (int seg = 0; seg < 5; seg++) {
                float segHeight = (height / 5);
                float segY = y + height - (seg * segHeight);
                float segAmplitude = amplitude * 5 - seg;

                if (segAmplitude > 0) {
                    // Different colors for different segments
                    switch (seg) {
                        case 4: GL11.glColor4f(1.0f, 0.0f, 0.0f, alpha); break; // Red
                        case 3: GL11.glColor4f(1.0f, 0.5f, 0.0f, alpha); break; // Orange
                        case 2: GL11.glColor4f(1.0f, 1.0f, 0.0f, alpha); break; // Yellow
                        case 1: GL11.glColor4f(0.0f, 1.0f, 0.0f, alpha); break; // Green
                        case 0: GL11.glColor4f(0.0f, 1.0f, 1.0f, alpha); break; // Cyan
                    }

                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex2f(px, segY);
                    GL11.glVertex2f(px + barWidth, segY);
                    GL11.glVertex2f(px + barWidth, segY - segHeight * Math.min(1, segAmplitude));
                    GL11.glVertex2f(px, segY - segHeight * Math.min(1, segAmplitude));
                    GL11.glEnd();
                }
            }
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void renderWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2.0f); // Make the line a bit thicker
        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (int i = 0; i < n; i++) {
            float px = x + (i * width) / (n - 1);
            float norm = Math.abs(points[i]) / 32768.0f; // 0 (low) to 1 (high)
            float r = norm;
            float g = 1.0f - norm;
            GL11.glColor4f(r, g, 0f, alpha);
            float py = y + height / 2 - (points[i] / (32768.0f)) * (height / 2);
            GL11.glVertex2f(px, py);
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }


    private static void renderParticleWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // Draw particles with varying sizes
        for (int i = 0; i < n; i++) {
            float amplitude = Math.abs(points[i]) / 32768.0f;
            float angle = (float) (i * Math.PI * 2 / n);

            // Create particle spread based on amplitude
            for (int p = 0; p < 5; p++) {
                float spread = (float) (Math.random() * amplitude * height * 0.3);
                float px = x + width/2 + (float)Math.cos(angle) * (width/2 * amplitude + spread);
                float py = y + height/2 + (float)Math.sin(angle) * (height/2 * amplitude + spread);

                GL11.glPointSize(1 + amplitude * 5);
                GL11.glBegin(GL11.GL_POINTS);
                GL11.glColor4f(0.5f + amplitude * 1f, 0.8f- amplitude * 1f, 1.0f - amplitude * 1f, alpha * (1 - p/5f));
                GL11.glVertex2f(px, py);
                GL11.glEnd();
            }
        }

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void renderGlowWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // Draw multiple lines with decreasing alpha for glow effect
        for (int layer = 0; layer < 3; layer++) {
            float layerAlpha = MathHelper.lerp(1f, 0.3f, layer * 0.3f) * alpha;
            float layerWidth = MathHelper.lerp(2, 7, layer * 0.3f);

            GL11.glLineWidth(layerWidth);
            GL11.glBegin(GL11.GL_LINE_STRIP);

            for (int i = 0; i < n; i++) {
                float px = x + (i * width) / (n - 1);
                float py = y + height / 2 - (points[i] / (32768.0f)) * (height / 2);

                GL11.glColor4f(0.3f, 0.7f, 1.0f, layerAlpha);
                GL11.glVertex2f(px, py);
            }

            GL11.glEnd();
        }

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }


    private static void renderOscilloscopeWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        // Draw grid
        GL11.glLineWidth(1.0f);
        GL11.glColor4f(0.2f, 0.2f, 0.2f, alpha * 0.5f);

        // Vertical grid lines
        for (int i = 0; i <= 10; i++) {
            float gx = x + (width * i / 10);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(gx, y);
            GL11.glVertex2f(gx, y + height);
            GL11.glEnd();
        }

        // Horizontal grid lines
        for (int i = 0; i <= 4; i++) {
            float gy = y + (height * i / 4);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(x, gy);
            GL11.glVertex2f(x + width, gy);
            GL11.glEnd();
        }

        // Draw waveform
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glColor4f(0.0f, 1.0f, 0.0f, alpha);

        for (int i = 0; i < n; i++) {
            float px = x + (i * width) / (n - 1);
            float amplitude = points[i] / 32768.0f;
            float py = y + height/2 + amplitude * height/2;
            GL11.glVertex2f(px, py);
        }

        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void renderDotWaveform(int[] points, float x, float y, float width, float height, float alpha) {
        int n = points.length;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
        GL11.glPointSize(3.0f);
        //preserve scale even when far


        GL11.glBegin(GL11.GL_POINTS);

        for (int i = 0; i < n; i++) {
            float px = x + (i * width) / (n - 1);
            float amplitude = Math.abs(points[i]) / 32768.0f;
            float py = y + height/2 - (amplitude) * (height / 2);

            GL11.glColor4f(amplitude, 0.2f + 0.8f * (1.0f - amplitude), 1.0f, alpha);
            GL11.glVertex2f(px, py);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}
