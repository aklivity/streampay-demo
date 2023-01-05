<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 20px;">
    <div class="items-center text-primary text-h4" style="margin-left: 40%; margin-bottom: 20px;">
      Activities
    </div>
    <q-table
      ref="tableRef"
      title-class="feed-title"
      hide-bottom
      hide-header
      card-style="box-shadow: none;"
      :rows="rows"
      :columns="columns"
      :table-colspan="9"
      row-key="index"
      virtual-scroll
      :virtual-scroll-item-size="48"
      :rows-per-page-options="[0]"
    >
       <template v-slot:body="props">
         <q-tr :props="props" no-hover>
           <q-td  key="transaction" :props="props">
             <q-avatar>
               <img src="https://cdn.quasar.dev/img/avatar.png">
             </q-avatar>
             <div>
               <b>You</b> paid <b>Somebody</b>
             </div>
             <div class="text-subtitle2">
               Jul 1, 2022
             </div>
           </q-td>

           <q-td
             key="amount"
             :props="props"
           >
             <div class="text-negative">
             {{ props.row.amount }}
             </div>
           </q-td>
         </q-tr>
       </template>
    </q-table>
  </q-page>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {useAuth0} from "@auth0/auth0-vue";

export default defineComponent({
  name: 'MainPage',
  setup () {
    const auth0 = useAuth0();

    const tableRef = ref(null);

    const columns = [
      {
        name: 'transaction',
        required: true,
        align: 'left',
        field: 'transaction',
        format: (val: any) => `${val}`
      },
      { name: 'amount', align: 'right', field: 'amount', sortable: true },
    ]

    const seed = [
      {
        transaction: 'Frozen Yogurt',
        amount: "-$159"
      },
      {
        transaction: 'Ice cream sandwich',
        amount: "-$237"
      },
      {
        transaction: 'Eclair',
        amount: "-$262"
      }
    ]

    const seedSize = seed.length

    let rows = [] as any;
    for (let i = 0; i < 1; i++) {
      rows = rows.concat(seed.map((r, j) => ({ ...r, index: i * seedSize + j + 1 })))
    }


    return {
      auth0: auth0,
      tableRef,

      columns,
      rows,

      pagination: {
        rowsPerPage: 0
      }
    }
  }
});
</script>
