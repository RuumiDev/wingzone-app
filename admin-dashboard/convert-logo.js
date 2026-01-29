import fs from 'fs';
import Canvas from 'canvas';
const { createCanvas, loadImage } = Canvas;

async function convertToBW() {
  try {
    // Load the original logo
    const image = await loadImage('./public/WingzoneLogo.png');
    
    // Create smaller canvas for thermal printer (250px wide)
    const canvas = createCanvas(250, 150);
    const ctx = canvas.getContext('2d');
    
    // Draw scaled image
    ctx.drawImage(image, 0, 0, 250, 150);
    
    // Get image data
    const imageData = ctx.getImageData(0, 0, 250, 150);
    const data = imageData.data;
    
    // Convert to black and white (threshold at 128)
    for (let i = 0; i < data.length; i += 4) {
      const avg = (data[i] + data[i + 1] + data[i + 2]) / 3;
      const bw = avg > 128 ? 255 : 0;
      data[i] = data[i + 1] = data[i + 2] = bw;
    }
    
    ctx.putImageData(imageData, 0, 0);
    
    // Save as PNG
    const buffer = canvas.toBuffer('image/png');
    fs.writeFileSync('./public/WingzoneLogo-BW.png', buffer);
    console.log('Created WingzoneLogo-BW.png (250x150, B&W)');
    
    // Also save to dist
    if (fs.existsSync('./dist')) {
      fs.writeFileSync('./dist/WingzoneLogo-BW.png', buffer);
      console.log('Copied to dist/WingzoneLogo-BW.png');
    }
  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  }
}

convertToBW();
