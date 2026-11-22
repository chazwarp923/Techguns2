"""Ports 1.12.2 Techguns model classes (ModelRenderer based) to the 1.21.1 shim classes.

Usage: python tools/port_models.py <legacy dir> <target dir> [files...]
"""
import os
import re
import sys

IMPORTS = {
    'net.minecraft.entity.Entity': 'net.minecraft.world.entity.Entity',
    'net.minecraft.entity.EntityLivingBase': 'net.minecraft.world.entity.LivingEntity',
    'net.minecraft.entity.player.EntityPlayer': 'net.minecraft.world.entity.player.Player',
    'net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType': 'net.minecraft.world.item.ItemDisplayContext',
    'net.minecraft.client.renderer.block.model.ItemCameraTransforms': None,
    'net.minecraft.client.model.ModelRenderer': 'techguns.client.models.ModelRenderer',
    'net.minecraft.client.model.ModelBase': 'techguns.client.models.ModelBase',
    'net.minecraft.client.model.ModelBox': None,
    'net.minecraft.client.renderer.GlStateManager': 'techguns.client.render.gl.GlStateManager',
    'net.minecraft.util.ResourceLocation': 'net.minecraft.resources.ResourceLocation',
    'net.minecraft.item.ItemStack': 'net.minecraft.world.item.ItemStack',
    'net.minecraft.item.Item': 'net.minecraft.world.item.Item',
    'net.minecraft.util.math.MathHelper': 'net.minecraft.util.Mth',
    'net.minecraft.client.Minecraft': 'net.minecraft.client.Minecraft',
}

RENAMES = [
    (r'\bItemCameraTransforms\.TransformType\b', 'ItemDisplayContext'),
    (r'\bTransformType\b', 'ItemDisplayContext'),
    (r'\bEntityLivingBase\b', 'LivingEntity'),
    (r'\bEntityPlayer\b', 'Player'),
    (r'\bMathHelper\b', 'Mth'),
    (r'@SideOnly\(Side\.CLIENT\)\s*', ''),
]


def port(text):
    out_imports = []
    body = []
    for line in text.split('\n'):
        m = re.match(r'\s*import\s+(static\s+)?([\w.]+);', line)
        if m:
            name = m.group(2)
            if name in IMPORTS:
                if IMPORTS[name]:
                    out_imports.append('import ' + IMPORTS[name] + ';')
            elif name.startswith(('net.minecraftforge.fml.relauncher',)):
                pass
            else:
                out_imports.append(line.strip())
        else:
            body.append(line)
    s = '\n'.join(body)
    for pat, rep in RENAMES:
        s = re.sub(pat, rep, s)
    pkg_end = s.index(';') + 1
    imports = '\n'.join(sorted(set(out_imports)))
    return s[:pkg_end] + '\n\n' + imports + '\n' + s[pkg_end:]


if __name__ == '__main__':
    src_dir, dst_dir = sys.argv[1], sys.argv[2]
    files = sys.argv[3:] or [f for f in os.listdir(src_dir) if f.endswith('.java')]
    os.makedirs(dst_dir, exist_ok=True)
    for f in files:
        with open(os.path.join(src_dir, f), encoding='utf-8', errors='replace') as fh:
            text = fh.read()
        with open(os.path.join(dst_dir, f), 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(port(text))
        print('ported', f)
