"""Create a warm light-mode floral wallpaper from the existing licensed flower image.
It respects the original subject contours, but brightens the backdrop and petals.
The dark photo remains unchanged for dark theme and custom gallery is untouched.
"""
from pathlib import Path
from PIL import Image, ImageFilter, ImageChops
src=Path("hb-v18/app/src/main/res/drawable-nodpi/vb_flower.jpg")
dst=src.with_name("vb_flower_light.jpg")
im=Image.open(src).convert("RGB")
w,h=im.size
if w>1000:
    h=round(h*1000/w);w=1000
    im=im.resize((w,h),Image.Resampling.LANCZOS)
px=list(im.getdata())
pale=bytearray(w*h*3)
subject=bytearray(w*h)
for i,(r,g,b) in enumerate(px):
    x=i%w;y=i//w
    # Neutral warm-cream background with a very gentle vertical vignette.
    shift=(y/h)*10
    cream=(int(250-shift),int(244-shift),int(232-shift))
    # Saturated magenta petals of the preset original, not dark shadows.
    magenta=max(0.,min(1.,(r-g-9)/59.))*max(0.,min(1.,(b-g+15)/49.))
    magenta*=max(0.,min(1.,(r-35)/70.))
    subject[i]=int(min(255,magenta*248))
    j=i*3
    pale[j:j+3]=bytes(cream)
base=Image.frombytes("RGB",(w,h),bytes(pale))
mask=Image.frombytes("L",(w,h),bytes(subject)).filter(ImageFilter.GaussianBlur(radius=2.1))
# The original flower highlights map to pastel pink and pink-orange details.
petal=Image.new("RGB",(w,h))
q=bytearray(w*h*3)
for i,(r,g,b) in enumerate(px):
    light=(r+g+b)/765
    j=i*3
    q[j]=int(230+18*light)
    q[j+1]=int(164+37*light)
    q[j+2]=int(167+34*light)
petal=Image.frombytes("RGB",(w,h),bytes(q))
out=Image.composite(petal,base,mask)
out.save(dst,quality=90,optimize=True,subsampling=0)
assert dst.stat().st_size>10000
print("Light theme floral asset:",dst,out.size,dst.stat().st_size,"bytes")
