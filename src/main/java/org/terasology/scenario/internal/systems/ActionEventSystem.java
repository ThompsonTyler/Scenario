/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.scenario.internal.systems;

import org.slf4j.LoggerFactory;
import org.terasology.assets.management.AssetManager;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.chat.ChatMessageEvent;
import org.terasology.logic.common.DisplayNameComponent;
import org.terasology.logic.inventory.events.GiveItemEvent;
import org.terasology.network.ClientComponent;
import org.terasology.network.ColorComponent;
import org.terasology.registry.In;
import org.terasology.rendering.nui.Color;
import org.terasology.scenario.components.ScenarioArgumentContainerComponent;
import org.terasology.scenario.components.actions.ScenarioSecondaryGiveBlockComponent;
import org.terasology.scenario.components.actions.ScenarioSecondaryGiveItemComponent;
import org.terasology.scenario.components.actions.ScenarioSecondaryLogInfoComponent;
import org.terasology.scenario.components.actions.ScenarioSecondarySendChatComponent;
import org.terasology.scenario.components.events.triggerInformation.InfoTriggeringEntityComponent;
import org.terasology.scenario.components.information.ScenarioValuePlayerComponent;
import org.terasology.scenario.internal.events.EventTriggerEvent;
import org.terasology.scenario.internal.events.evaluationEvents.EvaluateBlockEvent;
import org.terasology.scenario.internal.events.evaluationEvents.EvaluateIntEvent;
import org.terasology.scenario.internal.events.evaluationEvents.EvaluateItemPrefabEvent;
import org.terasology.scenario.internal.events.evaluationEvents.EvaluateStringEvent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemFactory;

import java.util.Map;

/**
 * Looks at Scenario logic entities that contain a secondary indicator for an action
 * The event that is looked for is {@link EventTriggerEvent} that will be sent from {@link ScenarioRootManagementSystem}
 * with the informationEntity being filled based on the triggered event
 *
 * This is the actual action result of triggering that action entity
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class ActionEventSystem extends BaseComponentSystem {
    @In
    private EntityManager entityManager;

    @In
    private BlockManager blockManager;

    private BlockItemFactory blockItemFactory;

    @In
    private AssetManager assetManager;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ActionEventSystem.class);

    @Override
    public void initialise() {
        super.initialise();
        blockItemFactory = new BlockItemFactory(entityManager);
    }

    @ReceiveEvent //Give Block
    public void onEventTriggerEvent(EventTriggerEvent event, EntityRef entity, ScenarioSecondaryGiveBlockComponent action) {
        Map<String, EntityRef> variables = entity.getComponent(ScenarioArgumentContainerComponent.class).arguments;

        EvaluateBlockEvent blockEvaluateEvent = new EvaluateBlockEvent(event.informationEntity);
        variables.get("block").send(blockEvaluateEvent);
        BlockFamily block = blockEvaluateEvent.getResult();

        EvaluateIntEvent intEvaluateEvent = new EvaluateIntEvent(event.informationEntity);
        variables.get("amount").send(intEvaluateEvent);
        int amount = intEvaluateEvent.getResult();

        EntityRef item = blockItemFactory.newInstance(block, amount);

        ScenarioValuePlayerComponent.PlayerType player = variables.get("player").getComponent(ScenarioValuePlayerComponent.class).type;
        if (player == ScenarioValuePlayerComponent.PlayerType.TRIGGERING_PLAYER) {
            EntityRef giveEntity = event.informationEntity.getComponent(InfoTriggeringEntityComponent.class).entity;
            GiveItemEvent giveItemEvent = new GiveItemEvent(giveEntity);
            item.send(giveItemEvent);
        }
    }

    @ReceiveEvent //Give Item
    public void onEventTriggerEvent(EventTriggerEvent event, EntityRef entity, ScenarioSecondaryGiveItemComponent action) {
        Map<String, EntityRef> variables = entity.getComponent(ScenarioArgumentContainerComponent.class).arguments;

        EvaluateItemPrefabEvent itemEvaluateEvent = new EvaluateItemPrefabEvent(event.informationEntity);
        variables.get("item").send(itemEvaluateEvent);
        Prefab itemPrefab = itemEvaluateEvent.getResult();

        EvaluateIntEvent intEvaluateEvent = new EvaluateIntEvent(event.informationEntity);
        variables.get("amount").send(intEvaluateEvent);
        int amount = intEvaluateEvent.getResult();

        ScenarioValuePlayerComponent.PlayerType player = variables.get("player").getComponent(ScenarioValuePlayerComponent.class).type;

        for (int i = 0; i < amount; i++) {
            EntityRef item = entityManager.create(itemPrefab);


            if (player == ScenarioValuePlayerComponent.PlayerType.TRIGGERING_PLAYER) {
                EntityRef giveEntity = event.informationEntity.getComponent(InfoTriggeringEntityComponent.class).entity;
                GiveItemEvent giveItemEvent = new GiveItemEvent(giveEntity);
                item.send(giveItemEvent);
            }
        }
    }

    @ReceiveEvent //Logger message
    public void onEventTriggerEvent(EventTriggerEvent event, EntityRef entity, ScenarioSecondaryLogInfoComponent action) {
        Map<String, EntityRef> variables = entity.getComponent(ScenarioArgumentContainerComponent.class).arguments;

        EvaluateStringEvent stringEvaluateEvent = new EvaluateStringEvent(event.informationEntity);
        variables.get("text").send(stringEvaluateEvent);
        String out = stringEvaluateEvent.getResult();

        logger.info(out);
    }

    @ReceiveEvent //Chat message
    public void onEventTriggerEvent(EventTriggerEvent event, EntityRef entity, ScenarioSecondarySendChatComponent action) {
        Map<String, EntityRef> variables = entity.getComponent(ScenarioArgumentContainerComponent.class).arguments;

        EvaluateStringEvent stringEvaluateEvent = new EvaluateStringEvent(event.informationEntity);
        variables.get("message").send(stringEvaluateEvent);
        String message = stringEvaluateEvent.getResult();

        EvaluateStringEvent stringEvaluateEvent2 = new EvaluateStringEvent(event.informationEntity);
        variables.get("owner").send(stringEvaluateEvent2);
        String from = stringEvaluateEvent2.getResult();

        EntityRef chatMessageEntity = entityManager.create(assetManager.getAsset("scenario:scenarioChatEntity", Prefab.class).get());
        chatMessageEntity.getComponent(DisplayNameComponent.class).name = from;
        chatMessageEntity.saveComponent(chatMessageEntity.getComponent(DisplayNameComponent.class));
        chatMessageEntity.getComponent(ColorComponent.class).color = Color.CYAN;
        chatMessageEntity.saveComponent(chatMessageEntity.getComponent(ColorComponent.class));

        for (EntityRef client : entityManager.getEntitiesWith(ClientComponent.class)) {
            client.send(new ChatMessageEvent(message, chatMessageEntity));
        }
    }
}
