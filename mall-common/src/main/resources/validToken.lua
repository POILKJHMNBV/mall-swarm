local key = KEYS[1];
local token = ARGV[1];
if (redis.call('EXISTS', key) == 1 and redis.call('GET', key) == token) then
    -- token is valid, delete it
    redis.call('DEL', key);
    return 0;
end
if (redis.call('EXISTS', key) == 0 or redis.call('GET', key) ~= token) then
    -- token is invalid
    return 1;
end