-- KEYS[1] buy order book (ZSET), KEYS[2] sell order book (ZSET)
-- Returns {buyOrderId, sellOrderId, sellPriceTicks} when the top of book crosses, otherwise empty list.

local bestBuy = redis.call('ZREVRANGE', KEYS[1], 0, 0, 'WITHSCORES')
if #bestBuy == 0 then
    return {}
end

local bestSell = redis.call('ZRANGE', KEYS[2], 0, 0, 'WITHSCORES')
if #bestSell == 0 then
    return {}
end

local buyPrice = tonumber(bestBuy[2])
local sellPrice = tonumber(bestSell[2])

if buyPrice >= sellPrice then
    return {bestBuy[1], bestSell[1], tostring(sellPrice)}
end

return {}
