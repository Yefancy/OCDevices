package ben_mkiv.ocdevices.client.renderer;

import ben_mkiv.ocdevices.client.models.ModelRack;
import ben_mkiv.ocdevices.common.blocks.BlockRack;
import ben_mkiv.ocdevices.common.tileentity.TileEntityRack;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.client.renderer.block.ServerRackModel;
import li.cil.oc.common.component.TerminalServer;
import li.cil.oc.server.component.DiskDriveMountable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.common.MinecraftForge;

import java.awt.*;
import java.util.HashMap;

public class RenderRack extends TileEntitySpecialRenderer<TileEntityRack> {
    private static ModelRack modelRack = new ModelRack();
    private static ServerRackModel ocModel;
    private static TextureAtlasSprite[] rackTextures;

    public static TextureAtlasSprite ocServerTex;

    public static HashMap<String, HashMap<String, TextureAtlasSprite>> textures = new HashMap<>();

    public static void init(){
        if(ocModel != null)
            return;

        ocModel = new ServerRackModel(Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(BlockRack.DEFAULTITEM.getDefaultState()));
        rackTextures = ocModel.serverTexture();
        ocServerTex = rackTextures[2];
    }

    @Override
    public void render(TileEntityRack rack,  double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        init();

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        switch(rack.yaw()) {
            case EAST: GlStateManager.rotate(-90, 0, 1, 0); break;
            case SOUTH: GlStateManager.rotate(180, 0, 1, 0); break;
            case WEST: GlStateManager.rotate(90, 0, 1, 0); break;
        }
        GlStateManager.translate(-0.5, -0.5, -0.5);

        bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1, 1, 0.95);
        GlStateManager.translate(0, 0, 1f/16);

        for(int rackSlot=0; rackSlot < rack.getSizeInventory(); rackSlot++) {
            if(rack.getStackInSlot(rackSlot).isEmpty())
                continue;

            RenderHelper.disableStandardItemLighting();
            //int l = rack.world().getCombinedLight(rack.getPos(), 0);
            //OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l % 65536, l / 65536);

            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE); //required as computronics binds another texture when boards are active
            GlStateManager.pushAttrib();
            renderServerBox(rack, rackSlot);
            renderStatusLED(rack, rackSlot);
            GlStateManager.popAttrib();
        }
        GlStateManager.popMatrix();

        // render rack door in world
        modelRack.render(rack, 0.0625F, null);

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void renderServerBox(TileEntityRack rack, int rackSlot){
        int colorValue = rack.getServerColor(rackSlot);

        Color color = new Color(colorValue);


        if(colorValue != -14869215){ // != black
            rackTextures[2] = getMountableTexture(rack, rackSlot);
            GlStateManager.color(1f/255 * color.getRed(), 1f/255 * color.getGreen(), 1f/255 * color.getBlue(), 1);
        }
        else { // use original oc server textures if the mountable is black colored
            RackMountableRenderEvent.Block event = new RackMountableRenderEvent.Block(rack, rackSlot, rack.lastData()[rackSlot], EnumFacing.DOWN);
            MinecraftForge.EVENT_BUS.post(event);

            if(event.getFrontTextureOverride() != null)
                rackTextures[2] = event.getFrontTextureOverride();
            else
                rackTextures[2] = ocServerTex;
        }

        rackTextures[3] = rackTextures[4] = rackTextures[5] = rackTextures[2];

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tess.getBuffer();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_TEX);

        // finally put the data to the buffer
        for(BakedQuad quad : ocModel.bakeQuads(ocModel.Servers()[rackSlot], rackTextures, 0)) {
            LightUtil.renderQuadColor(bufferBuilder, quad, 0);
        }

        tess.draw();
        GlStateManager.color(1, 1, 1, 1);

    }

    private TextureAtlasSprite getMountableTexture(TileEntityRack rack, int slot){
        ItemStack stack = rack.getStackInSlot(slot);

        if(!stack.isEmpty()) {
            String id = stack.getItem().getRegistryName() + ":" + stack.getMetadata();
            HashMap<String, TextureAtlasSprite> map = textures.get(id);
            if(map != null){

                if(id.equals("computronics:oc_parts:10")){
                    NBTTagCompound nbt = stack.getTagCompound();
                    if(nbt != null && nbt.hasKey("oc:data") && nbt.getCompoundTag("oc:data").hasKey("m")) {
                        int mode = nbt.getCompoundTag("oc:data").getInteger("m");
                        if(map.containsKey("mode"+mode))
                            return map.get("mode"+mode);
                    }
                }

                return map.containsKey("default") ? map.get("default") : ocServerTex;
            }
        }

        return ocServerTex;
    }

    private void renderStatusLED(TileEntityRack rack, int rackSlot){
        float v0 = 0.125F + (float)rackSlot * 1F/16 * 3;
        float v1 = v0 + 0.1875F;

        GlStateManager.pushMatrix();
        GlStateManager.rotate(180, 0, 0, 1);
        GlStateManager.translate(-1, -1, 0.031);
        GlStateManager.scale(1, 1, -0.2);

        MinecraftForge.EVENT_BUS.post(new RackMountableRenderEvent.TileEntity(rack, rackSlot, rack.getMountableData(rackSlot), v0, v1));
        GlStateManager.popMatrix();
    }

}
