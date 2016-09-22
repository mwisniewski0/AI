/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package ch.idsia.agents;

import ch.idsia.agents.controllers.human.HumanKeyboardAgent;
import ch.idsia.utils.wox.serial.Easy;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Sergey Karakovskiy, firstname_at_idsia_dot_ch
 * Date: May 9, 2009
 * Time: 8:28:06 PM
 * Package: ch.idsia.controllers.agents
 */

public final class AgentsPool
{
private static Agent currentAgent = null;

public static void addAgent(Agent agent)
{
    agentsHashMap.put(agent.getName(), agent);
}

public static void addAgent(String agentWOXName) throws IllegalFormatException
{
    addAgent(loadAgent(agentWOXName));
}

public static Agent loadAgent(String name)
{
    Agent agent;
    try
    {
        if (name.endsWith(".py"))
            agent = new AmiCoAgent(name);
        else
            agent = (Agent) Class.forName(name).newInstance();
    }
    catch (ClassNotFoundException e)
    {
        System.out.println("[~ Mario AI ~] :" + name + " is not a class name; trying to load a wox definition with that name.");
        try
        {
            agent = (Agent) Easy.load(name);
        } catch (Exception ex)
        {
            System.err.println("[~ Mario AI ~] :" + name + " is not a wox definition");
            agent = null;
        }

        if (agent == null)
        {
            System.err.println("[~ Mario AI ~] : wox definition has not been found as well. Loading <HumanKeyboardAgent> instead");
            agent = new HumanKeyboardAgent();
        }
        System.out.println("[~ Mario AI ~] : agent = " + agent);
    }
    catch (Exception e)
    {
//            e.printStackTrace ();
        agent = new HumanKeyboardAgent();
        System.err.println("[~ Mario AI ~] : Agent is null. Loading agent with name " + name + " failed.");
        System.out.println("Agent has been set to default: " + agent);
//            System.exit (1);
    }

    return agent;
}

public static Collection<Agent> getAgentsCollection()
{
    return agentsHashMap.values();
}

public static Set<String> getAgentsNames()
{
    return AgentsPool.agentsHashMap.keySet();
}

public static Agent getAgentByName(String agentName)
{
    // There is only one case possible;
    Agent ret = AgentsPool.agentsHashMap.get(agentName);
    if (ret == null)
        ret = AgentsPool.agentsHashMap.get(agentName.split(":")[0]);
    return ret;
}

public static Agent getCurrentAgent()
{
    if (currentAgent == null)
        currentAgent = (Agent) getAgentsCollection().toArray()[0];
    return currentAgent;
}

public static void setCurrentAgent(Agent agent)
{
    currentAgent = agent;
}

public static void setCurrentAgent(String agentWOXName)
{
    setCurrentAgent(AgentsPool.loadAgent(agentWOXName));
}

static HashMap<String, Agent> agentsHashMap = new LinkedHashMap<String, Agent>();
}
