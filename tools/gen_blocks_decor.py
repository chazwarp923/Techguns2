"""One-shot port helper: flattens the 1.12.2 decorative meta-blocks into individual 1.21 blocks.
Writes blockstates, block item models, loot tables, tags and lang entries.
Run from the repo root: python tools/gen_blocks_decor.py
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, 'src/main/resources/assets/techguns')
D = os.path.join(ROOT, 'src/main/resources/data')
LEG = os.path.join(ROOT, 'legacy/lang')
LEG_BS = os.path.join(ROOT, 'legacy/blockstates')


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


def m(model):
    """1.12 blockstate model ref (implicit block/ folder) -> 1.21 ref."""
    ns, p = model.split(':') if ':' in model else ('minecraft', model)
    return f'{ns}:block/{p}'


BLOCKS = []


def add(name, kind, bs, val, langkey, **kw):
    BLOCKS.append(dict(name=name, kind=kind, bs=bs, val=val, lang=langkey, **kw))


for i, (n, lvl) in enumerate([('ore_copper', None), ('ore_tin', 1), ('ore_lead', 2), ('ore_titanium', 3), ('ore_uranium', 2)]):
    if lvl is None:
        continue  # copper ore: vanilla has it since 1.17
    add(n, 'cube', 'basicore', n, f'tile.techguns.basicore.{i}.name', tool=lvl, ore=n[4:])
for i, n in enumerate(['container_red', 'container_green', 'container_blue', 'container_orange', 'panel_large_border',
                       'steelframe_blue', 'steelframe_dark', 'steelframe_scaffold']):
    add(n, 'cube', 'metalpanel', n, f'tile.techguns.metalpanel.{i}.name', tool=1)
for i, n in enumerate(['panel', 'grate1', 'grate2', 'grey_dark', 'grey', 'grey_tiles', 'border_red', 'plate_black',
                       'plate_red', 'border_lava']):
    add('nethermetal_' + n, 'cube', 'nethermetal', n, f'tile.techguns.nethermetal.{i}.name', tool=1)
for i, n in enumerate(['concrete_brown', 'concrete_brown_light', 'concrete_grey', 'concrete_grey_dark',
                       'concrete_brown_pipes', 'concrete_brown_light_scaff']):
    add(n, 'cube', 'concrete', n, f'tile.techguns.concrete.{i}.name', tool=1)
for i, n in enumerate(['neontubes2', 'neontubes2_rotated', 'neontubes4', 'neontubes4_rotated', 'neonsquare_white']):
    add(n, 'cube', 'neonlights', n, f'tile.techguns.neonlights.{i}.name', tool=0)
for meta, n in [(0, 'yellow'), (6, 'white')]:
    add('lamp_' + n, 'lamp', 'lamp0', n, f'tile.techguns.lamp0.{meta}.name', tool=0, inv='lamp_inventory_' + n)
for meta, n in [(12, 'yellow'), (13, 'white')]:
    add('lantern_' + n, 'lantern', 'lamp0', n + '_lantern', f'tile.techguns.lamp0.{meta}.name', tool=0,
        inv='lamp_inventory_' + n + '_lantern')
for i, n in enumerate(['metal', 'shiny', 'rusty', 'carbon']):
    add('ladder_' + n, 'ladder', 'ladder0', n, f'tile.techguns.ladder0.{8 + i}.name', tool=0)
add('stairs_metal', 'stairs', 'stairs_metal', 'false', 'tile.techguns.stairs_metal.7.name', tool=1)
add('stairs_metal_2', 'stairs', 'stairs_metal', 'true', 'tile.techguns.stairs_metal.15.name', tool=1)
add('stairs_concrete', 'stairs', 'stairs_concrete', 'false', 'tile.techguns.stairs_concrete.15.name', tool=1)
add('stairs_concrete_2', 'stairs', 'stairs_concrete', 'true', 'tile.techguns.stairs_concrete.7.name', tool=1)
add('sandbags', 'sandbags', 'sandbags', None, 'tile.techguns.sandbags.name', tool=None, inv='sandbags_inventory')


def load_bs(name):
    p = os.path.join(LEG_BS, name + '.json')
    return json.load(open(p, encoding='utf-8'))


def strip_prop(key, prop):
    return ','.join(p for p in key.split(',') if not p.startswith(prop + '='))


def fix_apply(v):
    if isinstance(v, list):
        return [fix_apply(x) for x in v]
    v = dict(v)
    v['model'] = m(v['model'])
    return v


def split_variants(bs, prop, val):
    out = {}
    for k, v in bs['variants'].items():
        kv = dict(p.split('=') for p in k.split(','))
        if kv.get(prop) != val:
            continue
        out[strip_prop(k, prop)] = fix_apply(v)
    return {'variants': out}


def split_multipart(bs, prop, val):
    out = []
    for part in bs['multipart']:
        w = dict(part.get('when', {}))
        if prop in w:
            if w[prop] != val:
                continue
            del w[prop]
        e = {'apply': fix_apply(part['apply'])}
        if w:
            e['when'] = w
        out.append(e)
    return {'multipart': out}


used_bs = set()
for b in BLOCKS:
    bs = load_bs(b['bs'])
    used_bs.add(b['bs'])
    if b['kind'] == 'cube':
        new = split_variants(bs, 'type', b['val'])
        item_parent = list(new['variants'].values())[0]['model']
    elif b['kind'] == 'ladder':
        new = split_variants(bs, 'type', b['val'])
        item_parent = new['variants']['facing=south']['model']
    elif b['kind'] == 'stairs':
        new = split_variants(bs, 'type2', b['val'])
        item_parent = new['variants']['facing=east,half=bottom,shape=straight']['model']
    elif b['kind'] == 'lamp':
        mp = split_multipart(bs, 'lamp_type', b['val'])
        new = {'variants': {}}
        for p in mp['multipart']:
            new['variants']['facing=' + p['when']['facing']] = p['apply']
        item_parent = 'techguns:item/' + b['inv']
    elif b['kind'] == 'lantern':
        new = split_multipart(bs, 'lamp_type', b['val'])
        item_parent = 'techguns:item/' + b['inv']
    elif b['kind'] == 'sandbags':
        new = {'multipart': [dict(p, apply=fix_apply(p['apply'])) for p in bs['multipart']]}
        item_parent = 'techguns:item/' + b['inv']
    else:
        raise ValueError(b['kind'])
    wj(os.path.join(A, 'blockstates', b['name'] + '.json'), new)
    wj(os.path.join(A, 'models/item', b['name'] + '.json'), {'parent': item_parent})
    wj(os.path.join(D, 'techguns/loot_table/blocks', b['name'] + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': 'techguns:' + b['name']}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': 'techguns:blocks/' + b['name']})

# move the consumed legacy blockstates out of the resource tree
os.makedirs(LEG_BS, exist_ok=True)
names = {b['name'] for b in BLOCKS}
for name in used_bs:
    src = os.path.join(A, 'blockstates', name + '.json')
    if os.path.exists(src) and name not in names:
        os.replace(src, os.path.join(LEG_BS, name + '.json'))


def tag(path, values):
    p = os.path.join(D, path)
    old = json.load(open(p))['values'] if os.path.exists(p) else []
    wj(p, {'replace': False, 'values': sorted(set(old) | set(values))})


tag('minecraft/tags/block/mineable/pickaxe.json', ['techguns:' + b['name'] for b in BLOCKS if b['tool'] is not None])
for lvl, t in [(1, 'needs_stone_tool'), (2, 'needs_iron_tool'), (3, 'needs_diamond_tool')]:
    tag(f'minecraft/tags/block/{t}.json', ['techguns:' + b['name'] for b in BLOCKS if b.get('ore') and b['tool'] == lvl])
tag('minecraft/tags/block/climbable.json', ['techguns:' + b['name'] for b in BLOCKS if b['kind'] == 'ladder'])
tag('minecraft/tags/block/stairs.json', ['techguns:' + b['name'] for b in BLOCKS if b['kind'] == 'stairs'])
tag('minecraft/tags/item/stairs.json', ['techguns:' + b['name'] for b in BLOCKS if b['kind'] == 'stairs'])
ores = [b for b in BLOCKS if b.get('ore')]
for b in ores:
    tag(f"c/tags/block/ores/{b['ore']}.json", ['techguns:' + b['name']])
    tag(f"c/tags/item/ores/{b['ore']}.json", ['techguns:' + b['name']])
tag('c/tags/block/ores.json', ['#c:ores/' + b['ore'] for b in ores])
tag('c/tags/item/ores.json', ['#c:ores/' + b['ore'] for b in ores])

for code in ['en_us', 'de_de', 'ru_ru']:
    tiles = json.load(open(os.path.join(LEG, code + '_tiles.json'), encoding='utf-8'))
    lp = os.path.join(A, 'lang', code + '.json')
    lang = json.load(open(lp, encoding='utf-8'))
    for b in BLOCKS:
        if b['lang'] in tiles:
            lang['block.techguns.' + b['name']] = tiles[b['lang']]
    wj(lp, dict(sorted(lang.items())))

print(len(BLOCKS), 'blocks')
