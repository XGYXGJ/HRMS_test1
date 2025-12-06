// hr-personnel.js: 负责从 /api/hr/files 获取数据，执行批量生成、审核、删除等操作
(async function(){
    const tbody = document.getElementById('tbody');
    const qInput = document.getElementById('q');
    document.getElementById('btn-bulk').addEventListener('click', async ()=>{
        const prefix = prompt('请输入账号前缀（例如：hr-）:') || 'u';
        const count = parseInt(prompt('要生成多少个?'),10) || 0;
        if (count <= 0) return alert('数量无效');
        const res = await fetch('/api/hr/files/bulk-generate', {
            method: 'POST',
            headers: {'Content-Type':'application/json'},
            body: JSON.stringify({prefix, count})
        });
        const data = await res.json();
        alert('已生成 ' + data.count + ' 条记录');
        load();
    });

    qInput && qInput.addEventListener('input', ()=>load());

    async function load(){
        const res = await fetch('/api/hr/files');
        const list = await res.json();
        const q = (qInput && qInput.value || '').trim().toLowerCase();
        tbody.innerHTML = '';
        (list || []).forEach(item=>{
            // item is a Map<String,Object> from mapper selectFilesWithOrgName
            const fileId = item.fileId || item.FILE_ID || item.fileId;
            const name = item.name || item.NAME || '';
            const phone = item.phoneNumber || item.PHONE_NUMBER || item.phoneNumber || '';
            const orgName = item.orgName || item.ORGNAME || item.orgName || '';
            const audit = item.auditStatus || item.AUDIT_STATUS || item.auditStatus || '';
            const userId = item.userId || item.USER_ID;

            if (q && !(String(name).toLowerCase().includes(q) || String(phone).toLowerCase().includes(q))) return;

            const tr = document.createElement('tr');
            tr.innerHTML = `
        <td>${fileId || ''}</td>
        <td>${userId || '-'}</td>
        <td>${name}</td>
        <td>${phone}</td>
        <td>${orgName}</td>
        <td>${audit}</td>
        <td>
          <button class="btn btn-sm btn-success" data-id="${fileId}" onclick="approveFile(${fileId})">通过</button>
          <button class="btn btn-sm btn-danger" data-id="${fileId}" onclick="deleteFile(${fileId})">删除</button>
        </td>
      `;
            tbody.appendChild(tr);
        });
    }

    window.approveFile = async function(id){
        if (!confirm('确认审核通过？')) return;
        await fetch(`/api/hr/files/${id}/approve`, {method:'POST'});
        load();
    };

    window.deleteFile = async function(id){
        if (!confirm('确认删除（逻辑删除）？')) return;
        await fetch(`/api/hr/files/${id}`, {method:'DELETE'});
        load();
    };

    load();
})();